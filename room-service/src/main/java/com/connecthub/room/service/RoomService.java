package com.connecthub.room.service;

import com.connecthub.room.dto.request.AddMemberRequest;
import com.connecthub.room.dto.request.CreateRoomRequest;
import com.connecthub.room.dto.request.UpdateRoomRequest;
import com.connecthub.room.dto.response.MemberResponse;
import com.connecthub.room.dto.response.RoomResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Business contract for the Room Service.
 *
 * WHY an interface?
 *   Separating the contract from the implementation lets us swap
 *   implementations (e.g., for testing with mocks) without changing
 *   the controller or any calling code.
 */
public interface RoomService {

    /** Create a new room and auto-add the creator as OWNER */
    RoomResponse createRoom(UUID creatorId, CreateRoomRequest request);

    /** Get a single room by ID */
    RoomResponse getRoomById(UUID roomId);

    /** Get all rooms that a user is a member of */
    List<RoomResponse> getRoomsByUserId(UUID userId);

    /** Add a member to a room (only OWNER/ADMIN can do this) */
    MemberResponse addMember(UUID roomId, UUID requesterId, AddMemberRequest request);

    /** Remove a member from a room */
    void removeMember(UUID roomId, UUID requesterId, UUID targetUserId);

    /** Get all members of a room */
    List<MemberResponse> getMembers(UUID roomId);

    /** Update a member's last-read timestamp (for unread count) */
    void updateLastRead(UUID roomId, UUID userId);

    /** Remove a user from all rooms */
    void removeUserFromAllRooms(UUID userId);

    /** Delete a room (only OWNER can do this) */
    void deleteRoom(UUID roomId, UUID requesterId);

    /** Delete a room as Admin */
    void deleteRoomAsAdmin(UUID roomId);

    /** Find or create a DM room between two users */
    RoomResponse getOrCreateDirectRoom(UUID userId1, UUID userId2);

    /** Check if a user is a member of a room */
    boolean isMember(UUID roomId, UUID userId);

    /** Get all rooms (Admin) */
    List<RoomResponse> getAllRooms();

    /** Update room settings (OWNER/ADMIN only) */
    RoomResponse updateRoom(UUID roomId, UUID requesterId, UpdateRoomRequest request);

    /** Join a room via its invite token */
    RoomResponse joinByInviteToken(String token, UUID userId);

    /** Update the last message timestamp for sorting */
    void updateLastMessageAt(UUID roomId, LocalDateTime timestamp);

    /** Change a member's role (OWNER/ADMIN only) */
    MemberResponse changeMemberRole(UUID roomId, UUID requesterId, UUID targetUserId, com.connecthub.room.model.MemberRole newRole);

    /** Mute or unmute a member (OWNER/ADMIN only) */
    MemberResponse muteMember(UUID roomId, UUID requesterId, UUID targetUserId, boolean isMuted);

    /** Check if a member is muted */
    boolean isMuted(UUID roomId, UUID userId);

    /** Get total number of rooms */
    long getTotalRoomsCount();

    /** Get count of rooms by type (DIRECT, GROUP, CHANNEL) */
    long countRoomsByType(com.connecthub.room.model.RoomType type);
}
