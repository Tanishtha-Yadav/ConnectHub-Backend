package com.connecthub.room.service.impl;

import com.connecthub.room.dto.request.AddMemberRequest;
import com.connecthub.room.dto.request.CreateRoomRequest;
import com.connecthub.room.dto.response.MemberResponse;
import com.connecthub.room.dto.response.RoomResponse;
import com.connecthub.room.exception.DuplicateMemberException;
import com.connecthub.room.exception.RoomNotFoundException;
import com.connecthub.room.exception.UnauthorizedRoomAccessException;
import com.connecthub.room.model.*;
import com.connecthub.room.repository.RoomMemberRepository;
import com.connecthub.room.repository.RoomRepository;
import com.connecthub.room.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Room Service implementation.
 * Handles room creation, membership management, DM lookup, and access control.
 *
 * KEY DESIGN DECISIONS:
 * 1. The room creator is automatically added as OWNER.
 * 2. Only OWNER/ADMIN can add or remove members.
 * 3. DIRECT rooms are unique per user-pair (no duplicates).
 * 4. Deleting a room also removes all memberships.
 */
@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${MESSAGE_SERVICE_URL:http://localhost:8083}")
    private String messageServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${NOTIFICATION_SERVICE_URL:http://localhost:8086}")
    private String notificationServiceUrl;

    @org.springframework.beans.factory.annotation.Value("${AUTH_SERVICE_URL:http://localhost:8081}")
    private String authServiceUrl;

    @Autowired
    public RoomServiceImpl(RoomRepository roomRepository,
                           RoomMemberRepository memberRepository,
                           org.springframework.web.client.RestTemplate restTemplate) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.restTemplate = restTemplate;
    }

    // =========================================================================
    // Create Room
    // =========================================================================

    @Override
    public RoomResponse createRoom(UUID creatorId, CreateRoomRequest request) {
        // Step 1: Create the room entity
        Room room = new Room(request.getName(), request.getType(), creatorId);
        room.setDescription(request.getDescription());
        room.setMaxMemberLimit(request.getMaxMemberLimit());
        Room saved = roomRepository.save(room);

        // Step 2: Add the creator as OWNER
        RoomMember ownerMember = new RoomMember(saved.getRoomId(), creatorId, MemberRole.OWNER);
        memberRepository.save(ownerMember);

        // Step 3: Add any initial members specified in the request
        if (request.getMemberIds() != null) {
            int currentMembers = 1;
            for (UUID memberId : request.getMemberIds()) {
                if (room.getMaxMemberLimit() != null && currentMembers >= room.getMaxMemberLimit()) {
                    break; // Silently skip if limit is reached during creation
                }
                // Skip if the memberId is the creator (already added as OWNER)
                if (!memberId.equals(creatorId)) {
                    RoomMember member = new RoomMember(saved.getRoomId(), memberId, MemberRole.MEMBER);
                    memberRepository.save(member);
                    currentMembers++;
                    sendRoomInviteNotification(memberId, saved.getRoomId(), creatorId, saved.getName());
                }
            }
        }

        long memberCount = memberRepository.countByRoomId(saved.getRoomId());
        return RoomResponse.from(saved, memberCount);
    }

    // =========================================================================
    // Get Room
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));
        long memberCount = memberRepository.countByRoomId(roomId);
        RoomResponse response = RoomResponse.from(room, memberCount);

        // Also attach the member list
        List<MemberResponse> members = memberRepository.findByRoomId(roomId)
                .stream().map(MemberResponse::from).collect(Collectors.toList());
        response.setMembers(members);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByUserId(UUID userId) {
        return roomRepository.findRoomsByUserId(userId).stream()
                .map(room -> {
                    long count = memberRepository.countByRoomId(room.getRoomId());
                    RoomResponse res = RoomResponse.from(room, count);
                    
                    // Fetch unread count
                    try {
                        RoomMember member = memberRepository.findByRoomIdAndUserId(room.getRoomId(), userId).orElse(null);
                        if (member != null && member.getLastReadAt() != null) {
                            String url = String.format("%s/api/messages/room/%s/unread-count?userId=%s&lastReadAt=%s",
                                    messageServiceUrl, room.getRoomId(), userId, member.getLastReadAt().toString());
                            
                            // Propagate JWT token to avoid 403 Forbidden
                            String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                                    .getAuthentication().getCredentials();
                            
                            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                            headers.setBearerAuth(token);
                            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                            
                            org.springframework.http.ResponseEntity<java.util.Map> response = restTemplate.exchange(
                                    url, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
                            
                            java.util.Map<String, Object> resp = response.getBody();
                            if (resp != null && resp.containsKey("count")) {
                                res.setUnreadCount(((Number) resp.get("count")).longValue());
                            }
                        }
                    } catch (Exception e) {
                        // Log and ignore to avoid breaking the whole list
                        System.err.println("Failed to fetch unread count for room " + room.getRoomId() + ": " + e.getMessage());
                    }

                    // Dynamically resolve the other user's name for DIRECT rooms
                    if (room.getType() == RoomType.DIRECT) {
                        try {
                            RoomMember other = memberRepository.findByRoomId(room.getRoomId()).stream()
                                    .filter(m -> !m.getUserId().equals(userId))
                                    .findFirst()
                                    .orElse(null);
                            if (other != null) {
                                String authUrl = String.format("%s/api/auth/profile/%s", authServiceUrl, other.getUserId());
                                String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                                        .getAuthentication().getCredentials();
                                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                                headers.setBearerAuth(token);
                                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
                                org.springframework.http.ResponseEntity<java.util.Map> authResponse = restTemplate.exchange(
                                        authUrl, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
                                java.util.Map<String, Object> userBody = authResponse.getBody();
                                if (userBody != null) {
                                    String fullName = (String) userBody.get("fullName");
                                    String username = (String) userBody.get("username");
                                    res.setName((fullName != null && !fullName.trim().isEmpty()) ? fullName : username);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to fetch other user profile for room " + room.getRoomId() + ": " + e.getMessage());
                        }
                    }
                    
                    return res;
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Membership Management
    // =========================================================================

    @Override
    public MemberResponse addMember(UUID roomId, UUID requesterId, AddMemberRequest request) {
        // Verify the room exists
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        // Verify the requester is OWNER or ADMIN
        RoomMember requester = memberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedRoomAccessException("You are not a member of this room"));

        if (requester.getRole() == MemberRole.MEMBER) {
            throw new UnauthorizedRoomAccessException("Only OWNER or ADMIN can add members");
        }

        // Check for duplicate membership
        if (memberRepository.existsByRoomIdAndUserId(roomId, request.getUserId())) {
            throw new DuplicateMemberException("User is already a member of this room");
        }

        // Enforce max member limit
        if (room.getMaxMemberLimit() != null) {
            long currentMembers = memberRepository.countByRoomId(roomId);
            if (currentMembers >= room.getMaxMemberLimit()) {
                throw new IllegalStateException("Room has reached its maximum member limit");
            }
        }

        // Add the new member
        RoomMember newMember = new RoomMember(roomId, request.getUserId(), request.getRole());
        RoomMember saved = memberRepository.save(newMember);
        sendRoomInviteNotification(request.getUserId(), roomId, requesterId, room.getName());
        return MemberResponse.from(saved);
    }

    @Override
    public void removeMember(UUID roomId, UUID requesterId, UUID targetUserId) {
        // Verify the requester is OWNER or ADMIN (or is removing themselves)
        RoomMember requester = memberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedRoomAccessException("You are not a member of this room"));

        boolean isSelfRemoval = requesterId.equals(targetUserId);
        if (!isSelfRemoval && requester.getRole() == MemberRole.MEMBER) {
            throw new UnauthorizedRoomAccessException("Only OWNER or ADMIN can remove members");
        }

        memberRepository.deleteByRoomIdAndUserId(roomId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(UUID roomId) {
        return memberRepository.findByRoomId(roomId).stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Last Read / Unread Count
    // =========================================================================

    @Override
    public void updateLastRead(UUID roomId, UUID userId) {
        RoomMember member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RoomNotFoundException("Membership not found"));
        member.setLastReadAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    // =========================================================================
    // Delete Room
    // =========================================================================

    @Override
    @Transactional
    public void removeUserFromAllRooms(UUID userId) {
        java.util.List<Room> userRooms = roomRepository.findRoomsByUserId(userId);
        for (Room room : userRooms) {
            if (room.getType() == RoomType.DIRECT) {
                // Delete the entire DIRECT room — it's meaningless without both parties
                memberRepository.deleteAllByRoomId(room.getRoomId());
                roomRepository.delete(room);
            }
            // GROUP / CHANNEL rooms: DO NOT remove the membership.
            // By keeping the RoomMember record, the member count remains the same,
            // and the UI will render them as a "Deleted User" tombstone without presence.
        }
    }
    @Override
    public void deleteRoom(UUID roomId, UUID requesterId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        RoomMember requester = memberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedRoomAccessException("You are not a member of this room"));

        if (room.getType() != RoomType.DIRECT && requester.getRole() != MemberRole.OWNER && requester.getRole() != MemberRole.ADMIN) {
            throw new UnauthorizedRoomAccessException("Only the room OWNER or ADMIN can delete a room");
        }

        // Remove all members first, then the room
        memberRepository.deleteAllByRoomId(roomId);
        roomRepository.delete(room);
    }

    @Override
    public void deleteRoomAsAdmin(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        // Remove all members first, then the room
        memberRepository.deleteAllByRoomId(roomId);
        roomRepository.delete(room);
    }

    // =========================================================================
    // Direct Message (DM) Room
    // =========================================================================

    @Override
    public RoomResponse getOrCreateDirectRoom(UUID userId1, UUID userId2) {
        // Check if a DM room already exists between these two users
        List<Room> existing = roomRepository.findDirectRoomBetweenUsers(userId1, userId2);
        if (!existing.isEmpty()) {
            Room room = existing.get(0);
            long count = memberRepository.countByRoomId(room.getRoomId());
            return RoomResponse.from(room, count);
        }

        // No existing DM — create one
        Room dm = new Room("Direct Message", RoomType.DIRECT, userId1);
        Room saved = roomRepository.save(dm);

        memberRepository.save(new RoomMember(saved.getRoomId(), userId1, MemberRole.OWNER));
        memberRepository.save(new RoomMember(saved.getRoomId(), userId2, MemberRole.MEMBER));

        RoomResponse res = RoomResponse.from(saved, 2);
        try {
            String authUrl = String.format("%s/api/auth/profile/%s", authServiceUrl, userId2);
            String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getCredentials();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<java.util.Map> authResponse = restTemplate.exchange(
                    authUrl, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
            java.util.Map<String, Object> userBody = authResponse.getBody();
            if (userBody != null) {
                String fullName = (String) userBody.get("fullName");
                String username = (String) userBody.get("username");
                res.setName((fullName != null && !fullName.trim().isEmpty()) ? fullName : username);
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch other user profile for new DM: " + e.getMessage());
        }

        return res;
    }

    // =========================================================================
    // Membership Verification
    // =========================================================================

    @Override
    public boolean isMember(UUID roomId, UUID userId) {
        return memberRepository.findByRoomIdAndUserId(roomId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(room -> {
                    long count = memberRepository.countByRoomId(room.getRoomId());
                    return RoomResponse.from(room, count);
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Room Settings & Invites
    // =========================================================================

    @Override
    public RoomResponse updateRoom(UUID roomId, UUID requesterId, com.connecthub.room.dto.request.UpdateRoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));

        RoomMember requester = memberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedRoomAccessException("You are not a member of this room"));

        if (requester.getRole() == MemberRole.MEMBER) {
            throw new UnauthorizedRoomAccessException("Only OWNER or ADMIN can update room settings");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            room.setName(request.getName());
        }
        if (request.getDescription() != null) {
            room.setDescription(request.getDescription());
        }
        if (request.getAvatarUrl() != null) {
            room.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getMaxMemberLimit() != null) {
            room.setMaxMemberLimit(request.getMaxMemberLimit());
        }

        Room saved = roomRepository.save(room);
        long count = memberRepository.countByRoomId(saved.getRoomId());
        return RoomResponse.from(saved, count);
    }

    @Override
    public RoomResponse joinByInviteToken(String token, UUID userId) {
        Room room = roomRepository.findByInviteToken(token)
                .orElseThrow(() -> new RoomNotFoundException("Invalid invite token"));

        if (room.getType() == RoomType.DIRECT) {
            throw new IllegalStateException("Cannot join a DIRECT room via invite link");
        }

        if (memberRepository.existsByRoomIdAndUserId(room.getRoomId(), userId)) {
            // Already a member, just return the room
            long count = memberRepository.countByRoomId(room.getRoomId());
            return RoomResponse.from(room, count);
        }

        if (room.getMaxMemberLimit() != null) {
            long currentMembers = memberRepository.countByRoomId(room.getRoomId());
            if (currentMembers >= room.getMaxMemberLimit()) {
                throw new IllegalStateException("Room has reached its maximum member limit");
            }
        }

        memberRepository.save(new RoomMember(room.getRoomId(), userId, MemberRole.MEMBER));
        long count = memberRepository.countByRoomId(room.getRoomId());
        return RoomResponse.from(room, count);
    }

    @Override
    public void updateLastMessageAt(UUID roomId, LocalDateTime timestamp) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room != null) {
            room.setLastMessageAt(timestamp);
            roomRepository.save(room);
        }
    }

    @Override
    public MemberResponse changeMemberRole(UUID roomId, UUID requesterId, UUID targetUserId, MemberRole newRole) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (room.getType() == RoomType.DIRECT) {
            throw new IllegalArgumentException("Cannot change roles in direct messages");
        }

        RoomMember requester = memberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedRoomAccessException("You are not a member of this room"));

        if (requester.getRole() == MemberRole.MEMBER) {
            throw new UnauthorizedRoomAccessException("Only OWNER or ADMIN can change roles");
        }

        RoomMember target = memberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user is not a member of this room"));

        if (target.getRole() == MemberRole.OWNER) {
            throw new UnauthorizedRoomAccessException("Cannot change role of the room OWNER");
        }

        // Admin cannot modify the OWNER's role
        if (requester.getRole() == MemberRole.ADMIN && target.getRole() == MemberRole.OWNER) {
            throw new UnauthorizedRoomAccessException("ADMIN cannot modify the OWNER's role");
        }

        target.setRole(newRole);
        RoomMember updated = memberRepository.save(target);
        return MemberResponse.from(updated);
    }

    @Override
    public MemberResponse muteMember(UUID roomId, UUID requesterId, UUID targetUserId, boolean isMuted) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found"));

        if (room.getType() == RoomType.DIRECT) {
            throw new IllegalArgumentException("Cannot mute members in direct messages");
        }

        RoomMember requester = memberRepository.findByRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedRoomAccessException("You are not a member of this room"));

        if (requester.getRole() == MemberRole.MEMBER) {
            throw new UnauthorizedRoomAccessException("Only OWNER or ADMIN can mute members");
        }

        RoomMember target = memberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user is not a member of this room"));

        if (target.getRole() == MemberRole.OWNER) {
            throw new UnauthorizedRoomAccessException("Cannot mute the room OWNER");
        }

        target.setIsMuted(isMuted);
        RoomMember updated = memberRepository.save(target);
        return MemberResponse.from(updated);
    }

    @Override
    public boolean isMuted(UUID roomId, UUID userId) {
        return memberRepository.findByRoomIdAndUserId(roomId, userId)
                .map(RoomMember::getIsMuted)
                .orElse(false);
    }

    private void sendRoomInviteNotification(UUID targetUserId, UUID roomId, UUID fromUserId, String roomName) {
        try {
            String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getCredentials();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);

            String notifUrl = String.format("%s/api/notifications/send-internal", notificationServiceUrl);
            
            java.util.Map<String, Object> notifReq = new java.util.HashMap<>();
            notifReq.put("userId", targetUserId.toString());
            notifReq.put("type", "ROOM_INVITE");
            notifReq.put("title", "Room Invitation");
            notifReq.put("message", "You have been added to " + roomName);
            notifReq.put("roomId", roomId.toString());
            if (fromUserId != null) {
                notifReq.put("fromUserId", fromUserId.toString());
            }
            
            org.springframework.http.HttpEntity<java.util.Map> entity = new org.springframework.http.HttpEntity<>(notifReq, headers);
            restTemplate.postForObject(notifUrl, entity, java.util.Map.class);
        } catch (Exception e) {
            System.err.println("Failed to send room invite notification: " + e.getMessage());
        }
    }

    @Override
    public long getTotalRoomsCount() {
        return roomRepository.count();
    }

    @Override
    public long countRoomsByType(com.connecthub.room.model.RoomType type) {
        return roomRepository.countByType(type);
    }
}
