package com.connecthub.room.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Room entity — represents a chat room (DM, Group, or Channel).
 *
 * WHY separate Room and RoomMember tables?
 *   A room has metadata (name, type) while membership is a many-to-many
 *   relationship between users and rooms, each with its own role and timestamps.
 *   Splitting them keeps the schema normalized and queries fast.
 */
@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_room_type", columnList = "type"),
    @Index(name = "idx_room_created_by", columnList = "created_by")
})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "room_id", updatable = false, nullable = false)
    private UUID roomId;

    /** Display name for the room (e.g., "Project Alpha Chat") */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Optional description shown in room settings */
    @Column(name = "description", length = 500)
    private String description;

    /** DIRECT, GROUP, or CHANNEL */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RoomType type = RoomType.GROUP;

    /** URL to room avatar/icon image */
    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /** Timestamp of the most recent message sent in this room (for sorting) */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /** Maximum number of members allowed (null = unlimited) */
    @Column(name = "max_member_limit")
    private Integer maxMemberLimit;

    /** Shareable invite token for joining this room (UUID, unique per room) */
    @Column(name = "invite_token", unique = true)
    private String inviteToken;

    /** UUID of the user who created this room */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Room() {}

    public Room(String name, RoomType type, UUID createdBy) {
        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks — auto-set timestamps
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Auto-generate a unique invite token on room creation
        if (this.inviteToken == null) {
            this.inviteToken = UUID.randomUUID().toString().replace("-", "");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public UUID getRoomId()                         { return roomId; }
    public void setRoomId(UUID roomId)              { this.roomId = roomId; }

    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }

    public RoomType getType()                       { return type; }
    public void setType(RoomType type)              { this.type = type; }

    public String getAvatarUrl()                    { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl)      { this.avatarUrl = avatarUrl; }

    public LocalDateTime getLastMessageAt()         { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime t)   { this.lastMessageAt = t; }

    public Integer getMaxMemberLimit()              { return maxMemberLimit; }
    public void setMaxMemberLimit(Integer limit)    { this.maxMemberLimit = limit; }

    public String getInviteToken()                  { return inviteToken; }
    public void setInviteToken(String token)        { this.inviteToken = token; }

    public UUID getCreatedBy()                      { return createdBy; }
    public void setCreatedBy(UUID createdBy)        { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime t)       { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)       { this.updatedAt = t; }
}
