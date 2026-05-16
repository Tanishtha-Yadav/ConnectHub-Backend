package com.connecthub.room.dto.response;

import com.connecthub.room.model.Room;
import com.connecthub.room.model.RoomType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO returned to the client for room operations.
 * Hides internal JPA details and adds computed fields like memberCount.
 */
public class RoomResponse {

    private UUID roomId;
    private String name;
    private String description;
    private RoomType type;
    private String avatarUrl;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastMessageAt;
    private Integer maxMemberLimit;
    private String inviteToken;
    private long memberCount;
    private long unreadCount;
    private List<MemberResponse> members;

    /**
     * Factory method — converts a Room entity to a RoomResponse DTO.
     */
    public static RoomResponse from(Room room, long memberCount) {
        RoomResponse r = new RoomResponse();
        r.roomId = room.getRoomId();
        r.name = room.getName();
        r.description = room.getDescription();
        r.type = room.getType();
        r.avatarUrl = room.getAvatarUrl();
        r.createdBy = room.getCreatedBy();
        r.createdAt = room.getCreatedAt();
        r.updatedAt = room.getUpdatedAt();
        r.lastMessageAt = room.getLastMessageAt();
        r.maxMemberLimit = room.getMaxMemberLimit();
        r.inviteToken = room.getInviteToken();
        r.memberCount = memberCount;
        return r;
    }

    // ---- Getters & Setters ----

    public UUID getRoomId()                          { return roomId; }
    public void setRoomId(UUID roomId)               { this.roomId = roomId; }

    public String getName()                          { return name; }
    public void setName(String name)                 { this.name = name; }

    public String getDescription()                   { return description; }
    public void setDescription(String d)             { this.description = d; }

    public RoomType getType()                        { return type; }
    public void setType(RoomType type)               { this.type = type; }

    public String getAvatarUrl()                     { return avatarUrl; }
    public void setAvatarUrl(String url)             { this.avatarUrl = url; }

    public UUID getCreatedBy()                       { return createdBy; }
    public void setCreatedBy(UUID createdBy)         { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()              { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)        { this.updatedAt = t; }

    public LocalDateTime getLastMessageAt()          { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime t)    { this.lastMessageAt = t; }

    public Integer getMaxMemberLimit()               { return maxMemberLimit; }
    public void setMaxMemberLimit(Integer limit)     { this.maxMemberLimit = limit; }

    public String getInviteToken()                   { return inviteToken; }
    public void setInviteToken(String token)         { this.inviteToken = token; }

    public long getMemberCount()                     { return memberCount; }
    public void setMemberCount(long c)               { this.memberCount = c; }

    public long getUnreadCount()                     { return unreadCount; }
    public void setUnreadCount(long c)               { this.unreadCount = c; }

    public List<MemberResponse> getMembers()         { return members; }
    public void setMembers(List<MemberResponse> m)   { this.members = m; }
}
