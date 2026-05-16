package com.connecthub.room.controller;

import com.connecthub.room.dto.request.AddMemberRequest;
import com.connecthub.room.dto.request.CreateRoomRequest;
import com.connecthub.room.dto.request.UpdateRoomRequest;
import com.connecthub.room.dto.response.MemberResponse;
import com.connecthub.room.dto.response.RoomResponse;
import com.connecthub.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for room/channel management.
 * All endpoints (except health) require a valid JWT token.
 *
 * WHY do we extract userId from Authentication instead of request params?
 *   The JWT filter puts the userId into SecurityContext as the principal.
 *   Reading it from there prevents users from impersonating others by
 *   sending a fake userId in the request body.
 */
@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Room/Channel management endpoints")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // =========================================================================
    // POST /api/rooms — Create a new room
    // =========================================================================

    @PostMapping
    @Operation(summary = "Create a new room (GROUP, DIRECT, or CHANNEL)")
    public ResponseEntity<RoomResponse> createRoom(
            Authentication auth,
            @Valid @RequestBody CreateRoomRequest request) {
        UUID creatorId = UUID.fromString(auth.getName());
        RoomResponse response = roomService.createRoom(creatorId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================================
    // GET /api/rooms/{roomId} — Get room details
    // =========================================================================

    @GetMapping("/{roomId}")
    @Operation(summary = "Get room details including member list")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.getRoomById(roomId));
    }

    // =========================================================================
    // GET /api/rooms/user/{userId} — Get all rooms for a user
    // =========================================================================

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all rooms a user belongs to")
    public ResponseEntity<List<RoomResponse>> getUserRooms(@PathVariable UUID userId) {
        return ResponseEntity.ok(roomService.getRoomsByUserId(userId));
    }

    // =========================================================================
    // GET /api/rooms/my-rooms — Get rooms for the authenticated user
    // =========================================================================

    @GetMapping("/my-rooms")
    @Operation(summary = "Get all rooms for the currently authenticated user")
    public ResponseEntity<List<RoomResponse>> getMyRooms(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(roomService.getRoomsByUserId(userId));
    }

    // =========================================================================
    // GET /api/rooms/all — Get all rooms (Admin)
    // =========================================================================

    @GetMapping("/all")
    @Operation(summary = "Admin: Get all rooms on the platform")
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    // =========================================================================
    // POST /api/rooms/{roomId}/members — Add a member
    // =========================================================================

    @PostMapping("/{roomId}/members")
    @Operation(summary = "Add a member to a room (OWNER/ADMIN only)")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID roomId,
            Authentication auth,
            @Valid @RequestBody AddMemberRequest request) {
        UUID requesterId = UUID.fromString(auth.getName());
        MemberResponse response = roomService.addMember(roomId, requesterId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================================
    // DELETE /api/rooms/{roomId}/members/{userId} — Remove a member
    // =========================================================================

    @DeleteMapping("/{roomId}/members/{userId}")
    @Operation(summary = "Remove a member from a room (OWNER/ADMIN or self)")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            Authentication auth) {
        UUID requesterId = UUID.fromString(auth.getName());
        roomService.removeMember(roomId, requesterId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Member removed successfully");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PUT /api/rooms/{roomId}/members/{userId}/role — Change a member's role
    // =========================================================================

    @PutMapping("/{roomId}/members/{userId}/role")
    @Operation(summary = "Change a member's role (OWNER/ADMIN only)")
    public ResponseEntity<MemberResponse> changeMemberRole(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @RequestParam com.connecthub.room.model.MemberRole role,
            Authentication auth) {
        UUID requesterId = UUID.fromString(auth.getName());
        MemberResponse response = roomService.changeMemberRole(roomId, requesterId, userId, role);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PUT /api/rooms/{roomId}/members/{userId}/mute — Mute/unmute a member
    // =========================================================================

    @PutMapping("/{roomId}/members/{userId}/mute")
    @Operation(summary = "Mute or unmute a member (OWNER/ADMIN only)")
    public ResponseEntity<MemberResponse> muteMember(
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @RequestParam boolean isMuted,
            Authentication auth) {
        UUID requesterId = UUID.fromString(auth.getName());
        MemberResponse response = roomService.muteMember(roomId, requesterId, userId, isMuted);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/rooms/{roomId}/members — Get all members
    // =========================================================================

    @GetMapping("/{roomId}/members")
    @Operation(summary = "Get all members of a room")
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable UUID roomId) {
        return ResponseEntity.ok(roomService.getMembers(roomId));
    }

    // =========================================================================
    // PUT /api/rooms/{roomId}/read — Mark room as read
    // =========================================================================

    @PutMapping("/{roomId}/read")
    @Operation(summary = "Update lastReadAt for the authenticated user in a room")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable UUID roomId,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        roomService.updateLastRead(roomId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Room marked as read");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // DELETE /api/rooms/{roomId} — Delete a room
    // =========================================================================

    @DeleteMapping("/{roomId}")
    @Operation(summary = "Delete a room (OWNER only)")
    public ResponseEntity<Map<String, String>> deleteRoom(
            @PathVariable UUID roomId,
            Authentication auth) {
        UUID requesterId = UUID.fromString(auth.getName());
        roomService.deleteRoom(roomId, requesterId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Room deleted successfully");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/rooms/direct/{otherUserId} — Get or create DM
    // =========================================================================

    @PostMapping("/direct/{otherUserId}")
    @Operation(summary = "Find or create a direct-message room with another user")
    public ResponseEntity<RoomResponse> getOrCreateDM(
            @PathVariable UUID otherUserId,
            Authentication auth) {
        UUID myId = UUID.fromString(auth.getName());
        RoomResponse response = roomService.getOrCreateDirectRoom(myId, otherUserId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/rooms/{roomId}/verify-member — Verify room membership
    // =========================================================================

    @GetMapping("/{roomId}/verify-member")
    @Operation(summary = "Verify that a user is a member of a room")
    public ResponseEntity<Boolean> verifyMembership(
            @PathVariable UUID roomId,
            @RequestParam UUID userId) {
        boolean isMember = roomService.isMember(roomId, userId);
        if (!isMember) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "User is not a member of this room");
        }
        return ResponseEntity.ok(true);
    }

    // =========================================================================
    // GET /api/rooms/{roomId}/is-muted — Check if a user is muted
    // =========================================================================

    @GetMapping("/{roomId}/is-muted")
    @Operation(summary = "Verify if a user is muted in a room")
    public ResponseEntity<Boolean> isMuted(
            @PathVariable UUID roomId,
            @RequestParam UUID userId) {
        boolean isMuted = roomService.isMuted(roomId, userId);
        return ResponseEntity.ok(isMuted);
    }

    // =========================================================================
    // PUT /api/rooms/{roomId} — Update room settings
    // =========================================================================

    @PutMapping("/{roomId}")
    @Operation(summary = "Update room settings (OWNER/ADMIN only)")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable UUID roomId,
            Authentication auth,
            @Valid @RequestBody UpdateRoomRequest request) {
        UUID requesterId = UUID.fromString(auth.getName());
        RoomResponse response = roomService.updateRoom(roomId, requesterId, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST /api/rooms/join/{token} — Join via invite link
    // =========================================================================

    @PostMapping("/join/{token}")
    @Operation(summary = "Join a room using an invite token")
    public ResponseEntity<RoomResponse> joinRoom(
            @PathVariable String token,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        RoomResponse response = roomService.joinByInviteToken(token, userId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PUT /api/rooms/{roomId}/last-message — Internal endpoint for sorting
    // =========================================================================

    @PutMapping("/{roomId}/last-message")
    @Operation(summary = "Update lastMessageAt (Internal use by message-service)")
    public ResponseEntity<Void> updateLastMessageAt(
            @PathVariable UUID roomId,
            @RequestParam String timestamp) {
        // Simple internal endpoint. Should ideally be secured by internal service role.
        roomService.updateLastMessageAt(roomId, LocalDateTime.parse(timestamp));
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // GET /api/rooms/health — Health check (public)
    // =========================================================================

    @GetMapping("/health")
    @Operation(summary = "Room service health check")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Room Service is running");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
