package com.connecthub.room.dto.request;

import com.connecthub.room.model.MemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for adding a user to a room.
 */
public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    /** Role to assign — defaults to MEMBER if not provided */
    private MemberRole role = MemberRole.MEMBER;

    // ---- Getters & Setters ----

    public UUID getUserId()                 { return userId; }
    public void setUserId(UUID userId)      { this.userId = userId; }

    public MemberRole getRole()             { return role; }
    public void setRole(MemberRole role)    { this.role = role; }
}
