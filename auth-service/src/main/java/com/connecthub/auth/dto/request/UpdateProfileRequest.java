package com.connecthub.auth.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    private String avatarUrl;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Size(max = 255, message = "Status message must not exceed 255 characters")
    private String statusMessage;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // Constructors
    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String fullName, String avatarUrl, String bio) {
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
