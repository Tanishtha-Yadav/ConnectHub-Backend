package com.connecthub.message.dto.request;

import com.connecthub.message.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for sending a new message.
 */
public class SendMessageRequest {

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Sender ID is required")
    private UUID senderId;

    private String senderName;

    private String content;

    @NotNull(message = "Message type is required")
    private MessageType type = MessageType.TEXT;

    private String mediaUrl;

    private String thumbnailUrl;

    private UUID replyToMessageId;

    // Getters & Setters
    public UUID getRoomId()                        { return roomId; }
    public void setRoomId(UUID roomId)             { this.roomId = roomId; }

    public UUID getSenderId()                      { return senderId; }
    public void setSenderId(UUID senderId)         { this.senderId = senderId; }

    public String getSenderName()                  { return senderName; }
    public void setSenderName(String n)            { this.senderName = n; }

    public String getContent()                     { return content; }
    public void setContent(String content)         { this.content = content; }

    public MessageType getType()                   { return type; }
    public void setType(MessageType type)          { this.type = type; }

    public String getMediaUrl()                    { return mediaUrl; }
    public void setMediaUrl(String mediaUrl)       { this.mediaUrl = mediaUrl; }

    public String getThumbnailUrl()                { return thumbnailUrl; }
    public void setThumbnailUrl(String t)          { this.thumbnailUrl = t; }

    public UUID getReplyToMessageId()              { return replyToMessageId; }
    public void setReplyToMessageId(UUID id)       { this.replyToMessageId = id; }
}
