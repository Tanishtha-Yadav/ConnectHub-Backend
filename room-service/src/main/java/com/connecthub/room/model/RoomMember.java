package com.connecthub.room.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RoomMember entity — join table linking users to rooms.
 *
 * WHY a separate entity instead of @ManyToMany?
 *   We need extra columns on the relationship: role, joinedAt, lastReadAt.
 *   A pure @ManyToMany cannot hold these; a dedicated entity can.
 *
 * lastReadAt tracks the timestamp of the last message the user has seen,
 * enabling unread count calculations on the client side.
 */
@Entity
@Table(name = "room_members", indexes = {
    @Index(name = "idx_member_user", columnList = "user_id"),
    @Index(name = "idx_member_room", columnList = "room_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_room_user", columnNames = {"room_id", "user_id"})
})
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Which room this membership belongs to */
    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    /** Which user is a member */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Role inside this room: OWNER, ADMIN, or MEMBER */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MemberRole role = MemberRole.MEMBER;

    /** When the user joined the room */
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * Timestamp of the last message the user has read in this room.
     * Used to compute unread count: count messages with sentAt > lastReadAt.
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /** Whether the user is muted (cannot send messages) */
    @Column(name = "is_muted", nullable = false)
    private Boolean isMuted = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public RoomMember() {}

    public RoomMember(UUID roomId, UUID userId, MemberRole role) {
        this.roomId = roomId;
        this.userId = userId;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        this.lastReadAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public UUID getId()                              { return id; }
    public void setId(UUID id)                       { this.id = id; }

    public UUID getRoomId()                          { return roomId; }
    public void setRoomId(UUID roomId)               { this.roomId = roomId; }

    public UUID getUserId()                          { return userId; }
    public void setUserId(UUID userId)               { this.userId = userId; }

    public MemberRole getRole()                      { return role; }
    public void setRole(MemberRole role)             { this.role = role; }

    public LocalDateTime getJoinedAt()               { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt)  { this.joinedAt = joinedAt; }

    public LocalDateTime getLastReadAt()             { return lastReadAt; }
    public void setLastReadAt(LocalDateTime t)       { this.lastReadAt = t; }

    public Boolean getIsMuted()                      { return isMuted; }
    public void setIsMuted(Boolean isMuted)          { this.isMuted = isMuted; }
}
