package com.connecthub.auth.dto.response;

import java.util.UUID;

/**
 * Response sent after Google/OAuth2 registration
 * Indicates that user needs to set password and username before first login
 */
public class RegistrationSetupResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private String temporaryUsername; // Username generated from email, can be changed
    private String message;
    private boolean needsSetup; // Always true in this response

    public RegistrationSetupResponse(UUID userId, String email, String fullName, String temporaryUsername) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.temporaryUsername = temporaryUsername;
        this.message = "Please set your password and username to complete registration";
        this.needsSetup = true;
    }

    // Getters and setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTemporaryUsername() {
        return temporaryUsername;
    }

    public void setTemporaryUsername(String temporaryUsername) {
        this.temporaryUsername = temporaryUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isNeedsSetup() {
        return needsSetup;
    }

    public void setNeedsSetup(boolean needsSetup) {
        this.needsSetup = needsSetup;
    }
}
