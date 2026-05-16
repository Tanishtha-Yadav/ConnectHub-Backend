package com.connecthub.message.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Message entity — persists every chat message sent in a room.
 * Follows the case study spec: messageId, roomId, senderId, content,
 * type, mediaUrl, replyToMessageId, isEdited, isDeleted,
 * deliveryStatus (SENT → DELIVERED → READ), sentAt, editedAt.
 */
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_room_sent", columnList = "roomId, sentAt"),
    @Index(name = "idx_sender",    columnList = "senderId"),
    @Index(name = "idx_delivery",  columnList = "roomId, deliveryStatus")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id", updatable = false, nullable = false)
    private UUID messageId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    /** Denormalized sender display name — stored at send time to avoid auth-service call for history */
    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MessageType type = MessageType.TEXT;

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    /** messageId of the message being replied to (for threaded replies) */
    @Column(name = "reply_to_message_id")
    private UUID replyToMessageId;

    @Column(name = "is_edited", nullable = false)
    private Boolean isEdited = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_reactions", joinColumns = @JoinColumn(name = "message_id"))
    @MapKeyColumn(name = "emoji")
    @Column(name = "reaction_count")
    private java.util.Map<String, Integer> reactions = new java.util.HashMap<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Message() {
        this.sentAt = LocalDateTime.now();
    }

    public Message(UUID roomId, UUID senderId, String senderName, String content, MessageType type) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.type = type;
        this.sentAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
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
