package com.connecthub.room.dto.request;

import com.connecthub.room.model.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new room.
 * The client sends this JSON when calling POST /api/rooms.
 */
public class CreateRoomRequest {

    @NotBlank(message = "Room name is required")
    @Size(max = 100, message = "Room name must be 100 characters or less")
    private String name;

    @Size(max = 500, message = "Description must be 500 characters or less")
    private String description;

    @NotNull(message = "Room type is required (DIRECT, GROUP, CHANNEL)")
    private RoomType type;

    /** IDs of users to add as initial members (besides the creator) */
    private List<UUID> memberIds;

    /** Maximum number of members allowed (null = unlimited) */
    private Integer maxMemberLimit;

    // ---- Getters & Setters ----

    public String getName()                    { return name; }
    public void setName(String name)           { this.name = name; }

    public String getDescription()             { return description; }
    public void setDescription(String d)       { this.description = d; }

    public RoomType getType()                  { return type; }
    public void setType(RoomType type)         { this.type = type; }

    public List<UUID> getMemberIds()           { return memberIds; }
    public void setMemberIds(List<UUID> ids)   { this.memberIds = ids; }

    public Integer getMaxMemberLimit()         { return maxMemberLimit; }
    public void setMaxMemberLimit(Integer limit) { this.maxMemberLimit = limit; }
}
