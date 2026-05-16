package com.connecthub.message.service;

import com.connecthub.message.dto.request.EditMessageRequest;
import com.connecthub.message.dto.request.SendMessageRequest;
import com.connecthub.message.dto.request.UpdateDeliveryStatusRequest;
import com.connecthub.message.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Business contract for the Message Service.
 * All message lifecycle operations are declared here.
 */
public interface MessageService {

    /**
     * Persist and return a new message.
     */
    MessageResponse sendMessage(SendMessageRequest request);

    /**
     * Retrieve a single message by ID (throws if not found or soft-deleted).
     */
    MessageResponse getMessageById(UUID messageId);

    /**
     * Retrieve paginated messages in a room, newest first.
     */
    Page<MessageResponse> getMessagesByRoom(UUID roomId, Pageable pageable);

    /**
     * Retrieve paginated messages before a given timestamp — supports infinite scroll "load more".
     */
    Page<MessageResponse> getMessagesBefore(UUID roomId, LocalDateTime before, Pageable pageable);

    /**
     * Retrieve all media messages (images and files) in a room.
     */
    List<MessageResponse> getMediaMessagesByRoom(UUID roomId);

    /**
     * Edit a message's content. Sets isEdited=true and updates editedAt.
     */
    MessageResponse editMessage(UUID messageId, UUID requesterId, EditMessageRequest request);

    /**
     * Soft-delete a message (sets isDeleted=true, clears content for others).
     */
    MessageResponse deleteMessage(UUID messageId, UUID requesterId);

    /**
     * Admin delete a message permanently or soft-delete.
     */
    MessageResponse deleteMessageAsAdmin(UUID messageId);

    /**
     * Add a reaction to a message.
     */
    MessageResponse reactToMessage(UUID messageId, String emoji);

    /**
     * Full-text search within a room's messages.
     */
    Page<MessageResponse> searchMessages(UUID roomId, String keyword, Pageable pageable);

    /**
     * Bulk-update delivery status for messages in a room up to a given timestamp.
     * Returns the number of rows updated.
     */
    int updateDeliveryStatus(UpdateDeliveryStatusRequest request);

    /**
     * Count total non-deleted messages in a room.
     */
    long getMessageCount(UUID roomId);

    /**
     * Count total messages in the platform.
     */
    long getTotalMessageCount();

    /**
     * Retrieve all messages sent after the user's lastReadAt timestamp (unread messages).
     */
    List<MessageResponse> getUnreadMessages(UUID roomId, UUID userId, LocalDateTime lastReadAt);

    long countUnreadMessages(UUID roomId, UUID userId, LocalDateTime lastReadAt);

    /**
     * Permanently delete all messages in a room (admin: clear room history).
     */
    void clearRoomHistory(UUID roomId);

    /**
     * Verify that a user is a member of a room.
     * Throws UnauthorizedMessageAccessException if not a member.
     */
    void verifyRoomMembership(UUID userId, UUID roomId);

    /**
     * Pin or unpin a message.
     */
    MessageResponse pinMessage(UUID messageId, boolean isPinned);
}
