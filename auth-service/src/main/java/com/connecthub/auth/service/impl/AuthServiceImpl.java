package com.connecthub.auth.service.impl;

import com.connecthub.auth.dto.request.*;
import com.connecthub.auth.dto.response.LoginResponse;
import com.connecthub.auth.dto.response.UserResponse;
import com.connecthub.auth.exception.InvalidCredentialsException;
import com.connecthub.auth.exception.UserAlreadyExistsException;
import com.connecthub.auth.exception.UserNotFoundException;
import com.connecthub.auth.model.AuthProvider;
import com.connecthub.auth.model.User;
import com.connecthub.auth.model.UserStatus;
import com.connecthub.auth.repository.UserRepository;
import com.connecthub.auth.security.JwtTokenProvider;
import com.connecthub.auth.service.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse register(RegisterRequest registerRequest) {
        // Check if user already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use: " + registerRequest.getEmail());
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + registerRequest.getUsername());
        }

        // Create new user
        User user = new User(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                com.connecthub.auth.model.AuthProvider.EMAIL
        );

        if (registerRequest.getFullName() != null) {
            user.setFullName(registerRequest.getFullName());
        }

        // Set initial status and last seen
        user.setStatus(UserStatus.ONLINE);
        user.setLastSeenAt(LocalDateTime.now());

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT tokens so the user is automatically logged in
        String token = jwtTokenProvider.generateToken(savedUser.getUserId().toString(), savedUser.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getUserId().toString());

        return new LoginResponse(
                token,
                refreshToken,
                "User registered successfully",
                convertToUserResponse(savedUser)
        );
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("User account is inactive");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getUserId().toString(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId().toString());

        // Update last seen
        user.setLastSeenAt(LocalDateTime.now());
        user.setStatus(UserStatus.ONLINE);
        userRepository.save(user);

        return new LoginResponse(
                token,
                refreshToken,
                "Login successful",
                convertToUserResponse(user)
        );
    }

    @Override
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setLastSeenAt(LocalDateTime.now());
        user.setStatus(UserStatus.INVISIBLE);
        userRepository.save(user);
    }

    @Override
    public Boolean validateToken(String token) {
        try {
            boolean isValid = jwtTokenProvider.validateToken(token);
            if (isValid) {
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                Boolean isActive = userRepository.findById(UUID.fromString(userId))
                        .map(User::getIsActive)
                        .orElse(false);
                return Boolean.TRUE.equals(isActive) || isActive == null;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        return UUID.fromString(userId);
    }

    @Override
    public LoginResponse refreshToken(String oldToken) {
        if (!jwtTokenProvider.validateToken(oldToken)) {
            throw new InvalidCredentialsException("Invalid or expired token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(oldToken);
        UUID userUUID = UUID.fromString(userId);

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userUUID));
                
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("User account is suspended");
        }

        String newToken = jwtTokenProvider.generateToken(userId, user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        return new LoginResponse(
                newToken,
                newRefreshToken,
                "Token refreshed successfully",
                convertToUserResponse(user)
        );
    }

    @Override
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return convertToUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest updateProfileRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (updateProfileRequest.getFullName() != null) {
            user.setFullName(updateProfileRequest.getFullName());
        }

        if (updateProfileRequest.getUsername() != null && !updateProfileRequest.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(updateProfileRequest.getUsername())) {
                throw new UserAlreadyExistsException("Username already taken: " + updateProfileRequest.getUsername());
            }
            user.setUsername(updateProfileRequest.getUsername());
        }

        if (updateProfileRequest.getAvatarUrl() != null) {
            user.setAvatarUrl(updateProfileRequest.getAvatarUrl());
        }

        if (updateProfileRequest.getBio() != null) {
            user.setBio(updateProfileRequest.getBio());
        }

        if (updateProfileRequest.getStatusMessage() != null) {
            user.setStatusMessage(updateProfileRequest.getStatusMessage());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        return convertToUserResponse(updatedUser);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Verify passwords match
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new InvalidCredentialsException("New password and confirm password do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public List<UserResponse> searchUsers(String query) {
        System.out.println("Auth Service: Searching users for query: [" + query + "]");
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.searchByQuery(query.trim());
        System.out.println("Auth Service: Found " + users.size() + " users matching [" + query + "]");
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse updateStatus(UUID userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        return convertToUserResponse(updatedUser);
    }

    @Override
    public void recordLastSeen(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return convertToUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        return convertToUserResponse(user);
    }

    @Override
    public LoginResponse googleLogin(String idToken) {
        try {
            // Verify the ID token against Google's public keys
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new InvalidCredentialsException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            String email    = payload.getEmail();
            String googleId = payload.getSubject();
            String name     = (String) payload.get("name");
            String picture  = (String) payload.get("picture");

            // Find existing user or create a new one
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;
            String message;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                if (Boolean.FALSE.equals(user.getIsActive())) {
                    throw new InvalidCredentialsException("User account is inactive");
                }
                // Keep avatar up-to-date from Google profile
                if (picture != null && user.getAvatarUrl() == null) {
                    user.setAvatarUrl(picture);
                }
                user.setProviderId(googleId);
                user.setStatus(UserStatus.ONLINE);
                user.setLastSeenAt(LocalDateTime.now());
                userRepository.save(user);
                message = "Login successful";
            } else {
                throw new UserNotFoundException("Google account not registered. Please register first.");
            }

            String jwtToken    = jwtTokenProvider.generateToken(user.getUserId().toString(), user.getRole().name());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId().toString());

            return new LoginResponse(jwtToken, refreshToken, message, convertToUserResponse(user));

        } catch (InvalidCredentialsException e) {
            System.err.println("Google authentication failed (InvalidCredentialsException): " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Google authentication failed (Exception): " + e.getMessage());
            e.printStackTrace();
            throw new InvalidCredentialsException("Google authentication failed: " + e.getMessage());
        }
    }

    @Override
    public Boolean verifyPassword(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getStatus(),
                user.getProvider(),
                user.getIsActive(),
                user.getLastSeenAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        response.setRole(user.getRole() != null ? user.getRole().name() : null);
        response.setStatusMessage(user.getStatusMessage());

        boolean isPrime = false;
        if (user.getRole() == com.connecthub.auth.model.Role.ROLE_ADMIN) {
            isPrime = true;
        } else if (user.getPrimeExpirationDate() != null && user.getPrimeExpirationDate().isAfter(LocalDateTime.now())) {
            isPrime = true;
        }
        response.setIsPrime(isPrime);

        return response;
    }

    @Override
    public void suspendUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public void reactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
    }
}
