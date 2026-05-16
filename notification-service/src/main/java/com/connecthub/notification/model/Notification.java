package com.connecthub.notification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification entity — persists notification history.
 * Tracks whether the user has read the notification.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "from_user_id")
    private UUID fromUserId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "is_emailed", nullable = false)
    private boolean isEmailed = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // Constructors
    public Notification() {}
    public Notification(UUID userId, NotificationType type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
    }

    // Getters & Setters
    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID id) { this.notificationId = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID id) { this.userId = id; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType t) { this.type = t; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID id) { this.roomId = id; }
    public UUID getFromUserId() { return fromUserId; }
    public void setFromUserId(UUID id) { this.fromUserId = id; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean r) { this.isRead = r; }
    public boolean isEmailed() { return isEmailed; }
    public void setEmailed(boolean e) { this.isEmailed = e; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
