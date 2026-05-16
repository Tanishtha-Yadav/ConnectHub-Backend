package com.connecthub.message.dto.request;

import com.connecthub.message.model.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for updating the delivery status of messages in a room.
 * Used when a recipient's WebSocket session becomes active (DELIVERED)
 * or when they read up to a specific message (READ).
 */
public class UpdateDeliveryStatusRequest {

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    @NotNull(message = "Delivery status is required")
    private DeliveryStatus status;

    /** The ID of the message that was read. We update all messages up to its sentAt time. */
    private UUID upToMessageId;

    /** All messages sent up to (and including) this timestamp will be updated (legacy/alternative) */
    private LocalDateTime upTo;

    // Getters & Setters
    public UUID getRoomId()                    { return roomId; }
    public void setRoomId(UUID roomId)         { this.roomId = roomId; }

    public UUID getRecipientId()               { return recipientId; }
    public void setRecipientId(UUID id)        { this.recipientId = id; }

    public DeliveryStatus getStatus()          { return status; }
    public void setStatus(DeliveryStatus s)    { this.status = s; }

    public LocalDateTime getUpTo()             { return upTo; }
    public void setUpTo(LocalDateTime upTo)    { this.upTo = upTo; }

    public UUID getUpToMessageId()             { return upToMessageId; }
    public void setUpToMessageId(UUID upToMessageId) { this.upToMessageId = upToMessageId; }
}
