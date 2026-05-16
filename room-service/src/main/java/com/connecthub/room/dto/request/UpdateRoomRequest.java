package com.connecthub.room.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateRoomRequest {

    @Size(max = 100, message = "Room name must be 100 characters or less")
    private String name;

    @Size(max = 500, message = "Description must be 500 characters or less")
    private String description;

    private String avatarUrl;

    private Integer maxMemberLimit;

    // ---- Getters & Setters ----

    public String getName()                    { return name; }
    public void setName(String name)           { this.name = name; }

    public String getDescription()             { return description; }
    public void setDescription(String d)       { this.description = d; }

    public String getAvatarUrl()               { return avatarUrl; }
    public void setAvatarUrl(String url)       { this.avatarUrl = url; }

    public Integer getMaxMemberLimit()         { return maxMemberLimit; }
    public void setMaxMemberLimit(Integer limit) { this.maxMemberLimit = limit; }
}
