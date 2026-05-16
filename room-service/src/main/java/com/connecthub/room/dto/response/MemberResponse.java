package com.connecthub.room.dto.response;

import com.connecthub.room.model.MemberRole;
import com.connecthub.room.model.RoomMember;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a single room member.
 */
public class MemberResponse {

    private UUID userId;
    private MemberRole role;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private Boolean isMuted;

    public static MemberResponse from(RoomMember m) {
        MemberResponse r = new MemberResponse();
        r.userId = m.getUserId();
        r.role = m.getRole();
        r.joinedAt = m.getJoinedAt();
        r.lastReadAt = m.getLastReadAt();
        r.isMuted = m.getIsMuted();
        return r;
    }

    // ---- Getters & Setters ----

    public UUID getUserId()                          { return userId; }
    public void setUserId(UUID userId)               { this.userId = userId; }

    public MemberRole getRole()                      { return role; }
    public void setRole(MemberRole role)             { this.role = role; }

    public LocalDateTime getJoinedAt()               { return joinedAt; }
    public void setJoinedAt(LocalDateTime t)         { this.joinedAt = t; }

    public LocalDateTime getLastReadAt()             { return lastReadAt; }
    public void setLastReadAt(LocalDateTime t)       { this.lastReadAt = t; }

    public Boolean getIsMuted()                      { return isMuted; }
    public void setIsMuted(Boolean isMuted)          { this.isMuted = isMuted; }
}
