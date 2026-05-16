package com.connecthub.message.controller;

import com.connecthub.message.dto.request.EditMessageRequest;
import com.connecthub.message.dto.request.SendMessageRequest;
import com.connecthub.message.dto.request.UpdateDeliveryStatusRequest;
import com.connecthub.message.dto.response.MessageResponse;
import com.connecthub.message.exception.UnauthorizedMessageAccessException;
import com.connecthub.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller exposing all message endpoints at /api/messages.
 * Follows the ConnectHub case study spec with full CRUD, pagination, search,
 * delivery status updates, and admin history clearing.
 */
@RestController
@RequestMapping("/api/messages")
@Tag(name = "Messages", description = "Message lifecycle management")
public class MessageResource {

    private final MessageService messageService;

    @Autowired
    public MessageResource(MessageService messageService) {
        this.messageService = messageService;
    }

    // =========================================================================
    // Send a new message
    // POST /api/messages/send
    // =========================================================================

    @PostMapping("/send")
    @Operation(summary = "Send a new message to a room")
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication auth,
            @Valid @RequestBody SendMessageRequest request) {
        // SECURITY: Verify that the authenticated user matches the sender
        UUID authenticatedUserId = UUID.fromString(auth.getName());
        if (!authenticatedUserId.equals(request.getSenderId())) {
            throw new UnauthorizedMessageAccessException(
                "You cannot send messages on behalf of another user");
        }
        MessageResponse response = messageService.sendMessage(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================================
    // Get a single message by ID
    // GET /api/messages/{messageId}
    // =========================================================================

    @GetMapping("/{messageId}")
    @Operation(summary = "Get a single message by ID")
    public ResponseEntity<MessageResponse> getMessageById(
            @PathVariable UUID messageId) {
        MessageResponse response = messageService.getMessageById(messageId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Get paginated messages for a room (newest first)
    // GET /api/messages/room/{roomId}?page=0&size=20
    // =========================================================================

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get paginated messages for a room (newest first)")
    public ResponseEntity<Page<MessageResponse>> getMessagesByRoom(
            Authentication auth,
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        // SECURITY: Verify user is a member of the room before returning messages
        UUID userId = UUID.fromString(auth.getName());
        messageService.verifyRoomMembership(userId, roomId);
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponse> response = messageService.getMessagesByRoom(roomId, pageable);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Get media messages for a room (gallery)
    // GET /api/messages/room/{roomId}/media
    // =========================================================================

    @GetMapping("/room/{roomId}/media")
    @Operation(summary = "Get all media messages for a room (Gallery)")
    public ResponseEntity<List<MessageResponse>> getMediaMessages(
            Authentication auth,
            @PathVariable UUID roomId) {
        UUID userId = UUID.fromString(auth.getName());
        messageService.verifyRoomMembership(userId, roomId);
        List<MessageResponse> response = messageService.getMediaMessagesByRoom(roomId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Infinite scroll: get messages before a timestamp
    // GET /api/messages/room/{roomId}/before?before=2026-04-21T10:00:00&page=0&size=20
    // =========================================================================

    @GetMapping("/room/{roomId}/before")
    @Operation(summary = "Load more messages before a given timestamp (infinite scroll)")
    public ResponseEntity<Page<MessageResponse>> getMessagesBefore(
            Authentication auth,
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        // SECURITY: Verify user is a member of the room
        UUID userId = UUID.fromString(auth.getName());
        messageService.verifyRoomMembership(userId, roomId);
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponse> response = messageService.getMessagesBefore(roomId, before, pageable);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Edit a message
    // PUT /api/messages/{messageId}/edit?requesterId={uuid}
    // =========================================================================

    @PutMapping("/{messageId}/edit")
    @Operation(summary = "Edit an existing message (sender only)")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable UUID messageId,
            @RequestParam UUID requesterId,
            @Valid @RequestBody EditMessageRequest request) {
        MessageResponse response = messageService.editMessage(messageId, requesterId, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Soft-delete a message
    // DELETE /api/messages/{messageId}?requesterId={uuid}
    // =========================================================================

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Soft-delete a message (sender only)")
    public ResponseEntity<MessageResponse> deleteMessage(
            @PathVariable UUID messageId,
            @RequestParam UUID requesterId) {
        MessageResponse response = messageService.deleteMessage(messageId, requesterId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // React to a message
    // PUT /api/messages/{messageId}/react?emoji=👍
    // =========================================================================

    @PutMapping("/{messageId}/react")
    @Operation(summary = "Add an emoji reaction to a message")
    public ResponseEntity<MessageResponse> reactToMessage(
            @PathVariable UUID messageId,
            @RequestParam String emoji) {
        MessageResponse response = messageService.reactToMessage(messageId, emoji);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Pin/Unpin a message
    // PUT /api/messages/pin/{messageId}
    // =========================================================================

    @PutMapping("/pin/{messageId}")
    @Operation(summary = "Pin or unpin a message")
    public ResponseEntity<MessageResponse> pinMessage(
            @PathVariable UUID messageId,
            @RequestParam boolean isPinned) {
        MessageResponse response = messageService.pinMessage(messageId, isPinned);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Search messages in a room
    // GET /api/messages/room/{roomId}/search?keyword=hello&page=0&size=20
    // =========================================================================

    @GetMapping("/room/{roomId}/search")
    @Operation(summary = "Search messages within a room by keyword")
    public ResponseEntity<Page<MessageResponse>> searchMessages(
            @PathVariable UUID roomId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponse> response = messageService.searchMessages(roomId, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Update delivery status (DELIVERED / READ)
    // PUT /api/messages/delivery-status
    // =========================================================================

    @PutMapping("/delivery-status")
    @Operation(summary = "Bulk-update delivery status for messages in a room")
    public ResponseEntity<Map<String, Object>> updateDeliveryStatus(
            @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        int updated = messageService.updateDeliveryStatus(request);
        Map<String, Object> response = new HashMap<>();
        response.put("updatedCount", updated);
        response.put("message", "Delivery status updated successfully");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Message count for a room
    // GET /api/messages/room/{roomId}/count
    // =========================================================================

    @GetMapping("/room/{roomId}/count")
    @Operation(summary = "Count total messages in a room")
    public ResponseEntity<Map<String, Long>> getMessageCount(@PathVariable UUID roomId) {
        long count = messageService.getMessageCount(roomId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Unread messages for a user in a room
    // GET /api/messages/room/{roomId}/unread?userId={uuid}&lastReadAt=2026-04-21T10:00:00
    // =========================================================================

    @GetMapping("/room/{roomId}/unread")
    @Operation(summary = "Get unread messages for a user in a room since lastReadAt")
    public ResponseEntity<List<MessageResponse>> getUnreadMessages(
            @PathVariable UUID roomId,
            @RequestParam UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastReadAt) {
        List<MessageResponse> response = messageService.getUnreadMessages(roomId, userId, lastReadAt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}/unread-count")
    @Operation(summary = "Get count of unread messages for a user in a room since lastReadAt")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable UUID roomId,
            @RequestParam UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastReadAt) {
        long count = messageService.countUnreadMessages(roomId, userId, lastReadAt);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Admin: clear entire room history
    // DELETE /api/messages/room/{roomId}/history
    // =========================================================================

    @DeleteMapping("/room/{roomId}/history")
    @Operation(summary = "Admin: permanently delete all messages in a room")
    public ResponseEntity<Map<String, String>> clearRoomHistory(@PathVariable UUID roomId) {
        messageService.clearRoomHistory(roomId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Room history cleared successfully");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Health check
    // GET /api/messages/health
    // =========================================================================

    @GetMapping("/health")
    @Operation(summary = "Message service health check")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Message Service is running");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
