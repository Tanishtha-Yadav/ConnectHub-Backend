package com.connecthub.auth.controller;

import com.connecthub.auth.dto.request.*;
import com.connecthub.auth.dto.request.GoogleAuthRequest;
import com.connecthub.auth.dto.response.LoginResponse;
import com.connecthub.auth.dto.response.UserResponse;
import com.connecthub.auth.model.UserStatus;
import com.connecthub.auth.security.JwtTokenProvider;
import com.connecthub.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        LoginResponse response = authService.register(registerRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Logout user
     */
    @PostMapping("/logout/{userId}")
    public ResponseEntity<Map<String, String>> logout(@PathVariable UUID userId) {
        authService.logout(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Validate JWT token
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Boolean isValid = authService.validateToken(token);
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Refresh JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestHeader("Authorization") String token) {
        String cleanToken = token.replace("Bearer ", "");
        LoginResponse response = authService.refreshToken(cleanToken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get user profile by ID
     */
    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable UUID userId) {
        UserResponse response = authService.getUserById(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        UserResponse response = authService.updateProfile(userId, updateProfileRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Change password
     */
    @PostMapping("/password/{userId}")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        authService.changePassword(userId, changePasswordRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Search users by username, full name, or email.
     * Supports both `q` (preferred) and `username` (legacy) query parameters.
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "username", required = false) String legacyUsername) {
        String searchQuery = (query != null && !query.isBlank()) ? query : legacyUsername;
        List<UserResponse> response = authService.searchUsers(searchQuery);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Update online status
     */
    @PutMapping("/status/{userId}")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable UUID userId,
            @RequestParam UserStatus status) {
        UserResponse response = authService.updateStatus(userId, status);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Record last seen timestamp
     */
    @PostMapping("/last-seen/{userId}")
    public ResponseEntity<Map<String, String>> recordLastSeen(@PathVariable UUID userId) {
        authService.recordLastSeen(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Last seen updated");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get user by email
     */
    @GetMapping("/user-by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        UserResponse response = authService.getUserByEmail(email);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get user by username
     */
    @GetMapping("/user-by-username")
    public ResponseEntity<UserResponse> getUserByUsername(@RequestParam String username) {
        UserResponse response = authService.getUserByUsername(username);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get all users (Admin)
     */
    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> response = authService.getAllUsers();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Get current user profile (from JWT token)
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String cleanToken = token.replace("Bearer ", "");
        String userId = jwtTokenProvider.getUserIdFromToken(cleanToken);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserResponse response = authService.getUserById(UUID.fromString(userId));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Update current user profile (from JWT token)
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateCurrentProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String cleanToken = token.replace("Bearer ", "");
        String userId = jwtTokenProvider.getUserIdFromToken(cleanToken);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserResponse response = authService.updateProfile(UUID.fromString(userId), updateProfileRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Change password (from JWT token)
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changeCurrentPassword(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String cleanToken = token.replace("Bearer ", "");
        String userId = jwtTokenProvider.getUserIdFromToken(cleanToken);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        authService.changePassword(UUID.fromString(userId), changePasswordRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Update user status (from JWT token)
     */
    @PostMapping("/status")
    public ResponseEntity<UserResponse> updateCurrentStatus(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam UserStatus status) {
        if (token == null || token.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String cleanToken = token.replace("Bearer ", "");
        String userId = jwtTokenProvider.getUserIdFromToken(cleanToken);
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        UserResponse response = authService.updateStatus(UUID.fromString(userId), status);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Internal endpoint for inter-service calls — returns only email and fullName.
     * Uses simple types (String only) so RestTemplate.getForObject(..., Map.class)
     * never fails due to LocalDateTime or enum deserialization issues.
     * Permitted without JWT in SecurityConfig (/api/auth/** is open).
     */
    @GetMapping("/internal/user-info/{userId}")
    public ResponseEntity<Map<String, String>> getInternalUserInfo(@PathVariable UUID userId) {
        UserResponse user = authService.getUserById(userId);
        Map<String, String> info = new HashMap<>();
        info.put("email", user.getEmail() != null ? user.getEmail() : "");
        info.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        info.put("username", user.getUsername() != null ? user.getUsername() : "");
        return ResponseEntity.ok(info);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Auth Service is running");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Google OAuth2 sign-in / sign-up.
     * Accepts a Google ID token from the frontend (obtained via Google Sign-In SDK),
     * verifies it server-side, then creates or finds the user and returns a JWT.
     */
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        LoginResponse response = authService.googleLogin(request.getIdToken());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
