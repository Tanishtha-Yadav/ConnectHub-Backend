package com.connecthub.auth.service;

import com.connecthub.auth.dto.request.*;
import com.connecthub.auth.dto.response.LoginResponse;
import com.connecthub.auth.dto.response.UserResponse;
import com.connecthub.auth.model.UserStatus;

import java.util.List;
import java.util.UUID;

public interface AuthService {

    /**
     * Register a new user with email and password
     * Returns LoginResponse with JWT token so user is automatically logged in
     */
    LoginResponse register(RegisterRequest registerRequest);

    /**
     * Login user with email and password
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * Logout user
     */
    void logout(UUID userId);

    /**
     * Validate JWT token
     */
    Boolean validateToken(String token);

    /**
     * Get user ID from token
     */
    UUID getUserIdFromToken(String token);

    /**
     * Refresh JWT token
     */
    LoginResponse refreshToken(String oldToken);

    /**
     * Get user by ID
     */
    UserResponse getUserById(UUID userId);

    /**
     * Update user profile
     */
    UserResponse updateProfile(UUID userId, UpdateProfileRequest updateProfileRequest);

    /**
     * Change user password
     */
    void changePassword(UUID userId, ChangePasswordRequest changePasswordRequest);

    /**
     * Search users by username, full name, or email
     */
    List<UserResponse> searchUsers(String query);

    /**
     * Get all users
     */
    List<UserResponse> getAllUsers();

    /**
     * Update user online status
     */
    UserResponse updateStatus(UUID userId, UserStatus status);

    /**
     * Record user last seen timestamp
     */
    void recordLastSeen(UUID userId);

    /**
     * Get user by email
     */
    UserResponse getUserByEmail(String email);

    /**
     * Get user by username
     */
    UserResponse getUserByUsername(String username);

    /**
     * Verify current password for a user
     */
    Boolean verifyPassword(UUID userId, String password);

    /**
     * Authenticate or register a user via Google ID token (OAuth2).
     * If the Google account is new, a user record is created automatically.
     */
    LoginResponse googleLogin(String idToken);

    /**
     * Suspend user
     */
    void suspendUser(UUID userId);

    /**
     * Reactivate user
     */
    void reactivateUser(UUID userId);

    /**
     * Delete user permanently
     */
    void deleteUser(UUID userId);
}
