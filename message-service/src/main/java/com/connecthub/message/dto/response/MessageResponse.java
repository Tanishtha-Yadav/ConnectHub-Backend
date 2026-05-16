package com.connecthub.message.dto.response;

import com.connecthub.message.model.DeliveryStatus;
import com.connecthub.message.model.Message;
import com.connecthub.message.model.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a single message.
 * Returned on all message API responses to avoid exposing the JPA entity directly.
 */
public class MessageResponse {

    private UUID messageId;
    private UUID roomId;
    private UUID senderId;
    private String senderName;
    private String content;
    private MessageType type;
    private String mediaUrl;
    private String thumbnailUrl;
    private UUID replyToMessageId;
    private Boolean isEdited;
    private Boolean isDeleted;
    private Boolean isPinned;
    private DeliveryStatus deliveryStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime editedAt;

    private java.util.Map<String, Integer> reactions;

    // -------------------------------------------------------------------------
    // Factory method — create from entity
    // -------------------------------------------------------------------------
    public static MessageResponse from(Message m) {
        MessageResponse r = new MessageResponse();
        r.messageId        = m.getMessageId();
        r.roomId           = m.getRoomId();
        r.senderId         = m.getSenderId();
        r.senderName       = m.getSenderName();
        r.content          = m.getIsDeleted() ? null : m.getContent();
        r.type             = m.getType();
        r.mediaUrl         = m.getIsDeleted() ? null : m.getMediaUrl();
        r.thumbnailUrl     = m.getIsDeleted() ? null : m.getThumbnailUrl();
        r.replyToMessageId = m.getReplyToMessageId();
        r.isEdited         = m.getIsEdited();
        r.isDeleted        = m.getIsDeleted();
        r.isPinned         = m.getIsPinned();
        r.deliveryStatus   = m.getDeliveryStatus();
        r.sentAt           = m.getSentAt();
        r.editedAt         = m.getEditedAt();
        r.reactions        = m.getReactions();
        return r;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------
    public UUID getMessageId()                      { return messageId; }
    public void setMessageId(UUID messageId)        { this.messageId = messageId; }

    public UUID getRoomId()                         { return roomId; }
    public void setRoomId(UUID roomId)              { this.roomId = roomId; }

    public UUID getSenderId()                       { return senderId; }
    public void setSenderId(UUID senderId)          { this.senderId = senderId; }

    public String getSenderName()                   { return senderName; }
    public void setSenderName(String senderName)    { this.senderName = senderName; }

    public String getContent()                      { return content; }
    public void setContent(String content)          { this.content = content; }

    public MessageType getType()                    { return type; }
    public void setType(MessageType type)           { this.type = type; }

    public String getMediaUrl()                     { return mediaUrl; }
    public void setMediaUrl(String mediaUrl)        { this.mediaUrl = mediaUrl; }

    public String getThumbnailUrl()                 { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl){ this.thumbnailUrl = thumbnailUrl; }

    public UUID getReplyToMessageId()               { return replyToMessageId; }
    public void setReplyToMessageId(UUID id)        { this.replyToMessageId = id; }

    public Boolean getIsEdited()                    { return isEdited; }
    public void setIsEdited(Boolean isEdited)       { this.isEdited = isEdited; }

    public Boolean getIsDeleted()                   { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted)     { this.isDeleted = isDeleted; }

    public DeliveryStatus getDeliveryStatus()       { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus s) { this.deliveryStatus = s; }

    public LocalDateTime getSentAt()                { return sentAt; }
    public void setSentAt(LocalDateTime sentAt)     { this.sentAt = sentAt; }

    public LocalDateTime getEditedAt()              { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public java.util.Map<String, Integer> getReactions() { return reactions; }
    public void setReactions(java.util.Map<String, Integer> reactions) { this.reactions = reactions; }

    public Boolean getIsPinned()                    { return isPinned; }
    public void setIsPinned(Boolean isPinned)       { this.isPinned = isPinned; }
}
