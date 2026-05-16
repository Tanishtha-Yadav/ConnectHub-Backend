package com.connecthub.websocket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload exchanged for all real-time chat events over STOMP.
 *
 * CLIENT → /app/chat.send      (CHAT messages)
 * CLIENT → /app/chat.typing    (TYPING indicators)
 * CLIENT → /app/chat.read      (READ receipts)
 * CLIENT → /app/chat.reaction  (REACTION events)
 *
 * SERVER → /topic/room/{roomId} (broadcasts all of the above)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {

    public enum EventType {
        CHAT, TYPING, READ, DELIVERED, REACTION, EDIT, DELETE, PRESENCE_UPDATE
    }

    private EventType eventType = EventType.CHAT;

    // Identifiers
    private UUID messageId;          // populated after DB persist (returned in CHAT broadcasts)
    private UUID roomId;
    private UUID senderId;
    private String senderName;

    // Content
    private String content;
    private String type = "TEXT";    // TEXT / IMAGE / FILE
    private String mediaUrl;

    // Threading
    private UUID replyToMessageId;

    // Reaction / read-receipt targeting
    private UUID targetMessageId;
    private String reaction;          // emoji string e.g. "👍"

    // Delivery
    private String deliveryStatus = "SENT";  // SENT / DELIVERED / READ

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    // ---- Getters & Setters ----

    public EventType getEventType()                      { return eventType; }
    public void setEventType(EventType eventType)        { this.eventType = eventType; }

    public UUID getMessageId()                           { return messageId; }
    public void setMessageId(UUID messageId)             { this.messageId = messageId; }

    public UUID getRoomId()                              { return roomId; }
    public void setRoomId(UUID roomId)                   { this.roomId = roomId; }

    public UUID getSenderId()                            { return senderId; }
    public void setSenderId(UUID senderId)               { this.senderId = senderId; }

    public String getSenderName()                        { return senderName; }
    public void setSenderName(String senderName)         { this.senderName = senderName; }

    public String getContent()                           { return content; }
    public void setContent(String content)               { this.content = content; }

    public String getType()                              { return type; }
    public void setType(String type)                     { this.type = type; }

    public String getMediaUrl()                          { return mediaUrl; }
    public void setMediaUrl(String mediaUrl)             { this.mediaUrl = mediaUrl; }

    public UUID getReplyToMessageId()                    { return replyToMessageId; }
    public void setReplyToMessageId(UUID id)             { this.replyToMessageId = id; }

    public UUID getTargetMessageId()                     { return targetMessageId; }
    public void setTargetMessageId(UUID id)              { this.targetMessageId = id; }

    public String getReaction()                          { return reaction; }
    public void setReaction(String reaction)             { this.reaction = reaction; }

    public String getDeliveryStatus()                    { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public LocalDateTime getTimestamp()                  { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp)    { this.timestamp = timestamp; }

    private Boolean isEdited = false;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime editedAt;

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    // --- Presence fields (used by PRESENCE_UPDATE events) ---
    private Boolean isOnline;           // true = came online, false = went offline
    private String lastSeenAt;          // ISO timestamp string of when user was last seen
    private String status;              // online / away / dnd / offline

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    public String getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(String lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
