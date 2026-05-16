package com.connecthub.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification payload to be pushed to Redis.
 * Matches the structure expected by websocket-handler's RedisMessageSubscriber.
 */
public class NotificationMessage {

    public enum NotificationType {
        NEW_MESSAGE, MENTION, ROOM_INVITE, SYSTEM
    }

    private NotificationType type;
    private UUID fromUserId;
    private String fromUserName;
    private UUID roomId;
    private String roomName;
    private String preview;
    private LocalDateTime timestamp;

    public NotificationMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public UUID getFromUserId() { return fromUserId; }
    public void setFromUserId(UUID id) { this.fromUserId = id; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String name) { this.fromUserName = name; }

    public UUID getRoomId() { return roomId; }
    public void setRoomId(UUID roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String name) { this.roomName = name; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime t) { this.timestamp = t; }
}
