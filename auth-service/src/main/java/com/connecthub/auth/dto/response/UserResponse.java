package com.connecthub.auth.dto.response;

import com.connecthub.auth.model.AuthProvider;
import com.connecthub.auth.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponse {

    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private UserStatus status;
    private AuthProvider provider;
    private Boolean isActive;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String role;
    private String statusMessage;
    private Boolean isPrime;

    // Constructors
    public UserResponse() {
    }

    public UserResponse(UUID userId, String username, String email, String fullName,
                        String avatarUrl, String bio, UserStatus status, AuthProvider provider,
                        Boolean isActive, LocalDateTime lastSeenAt, LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.status = status;
        this.provider = provider;
        this.isActive = isActive;
        this.lastSeenAt = lastSeenAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Boolean getIsPrime() {
        return isPrime;
    }

    public void setIsPrime(Boolean isPrime) {
        this.isPrime = isPrime;
    }
}

