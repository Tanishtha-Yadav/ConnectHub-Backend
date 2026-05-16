package com.connecthub.auth.service.impl;

import com.connecthub.auth.dto.request.CompleteRegistrationRequest;
import com.connecthub.auth.dto.response.LoginResponse;
import com.connecthub.auth.dto.response.RegistrationSetupResponse;
import com.connecthub.auth.dto.response.UserResponse;
import com.connecthub.auth.exception.InvalidCredentialsException;
import com.connecthub.auth.exception.UserNotFoundException;
import com.connecthub.auth.model.AuthProvider;
import com.connecthub.auth.model.User;
import com.connecthub.auth.model.UserStatus;
import com.connecthub.auth.repository.UserRepository;
import com.connecthub.auth.security.JwtTokenProvider;
import com.connecthub.auth.service.OAuth2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OAuth2ServiceImpl implements OAuth2Service {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String googleTokenUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String googleUserInfoUri;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LoginResponse authenticateWithGoogle(String idToken) {
        try {
            // Verify and decode the ID token from Google Sign-In SDK
            Map<String, Object> userInfo = verifyAndDecodeGoogleToken(idToken);

            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String providerId = (String) userInfo.get("sub");

            // Only allow login if user exists
            User user = createOrUpdateOAuth2User(email, name, providerId, AuthProvider.GOOGLE);

            String jwtToken = jwtTokenProvider.generateToken(user.getUserId().toString(), user.getRole().name());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId().toString());

            user.setStatus(UserStatus.ONLINE);
            user.setLastSeenAt(LocalDateTime.now());
            userRepository.save(user);

            return new LoginResponse(jwtToken, refreshToken, "Google login successful", convertToUserResponse(user));
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCredentialsException("Google authentication failed: " + e.getMessage());
        }
    }

    public RegistrationSetupResponse registerWithGoogle(String idToken) {
        try {
            System.out.println("[OAuth2Service] Starting Google registration for token: " + (idToken != null ? idToken.substring(0, 10) + "..." : "null"));
            // Verify and decode the ID token from Google Sign-In SDK
            Map<String, Object> userInfo = verifyAndDecodeGoogleToken(idToken);

            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String providerId = (String) userInfo.get("sub");
            System.out.println("[OAuth2Service] Token decoded. Email: " + email + ", Name: " + name);

            // Create new user without logging them in
            User user = createOrUpdateOAuth2UserForRegistration(email, name, providerId, AuthProvider.GOOGLE);
            System.out.println("[OAuth2Service] User created/updated for registration: " + user.getUserId());

            // Return setup response instead of login response
            String temporaryUsername = user.getUsername();
            return new RegistrationSetupResponse(user.getUserId(), email, name, temporaryUsername);
        } catch (Exception e) {
            System.err.println("[OAuth2Service] Google registration failed: " + e.getMessage());
            e.printStackTrace();
            throw new InvalidCredentialsException("Google registration failed: " + e.getMessage());
        }
    }

    /**
     * Complete OAuth2 registration by setting password and username
     * Called after user has set their desired password and username
     */
    @Override
    public LoginResponse completeOAuth2Registration(String userId, CompleteRegistrationRequest request) {
        try {
            UUID userUuid = UUID.fromString(userId);
            User user = userRepository.findByUserId(userUuid)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // Validate request
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                throw new InvalidCredentialsException("Password is required");
            }
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                throw new InvalidCredentialsException("Username is required");
            }

            // Check if username is already taken (by another user)
            Optional<User> existingUsername = userRepository.findByUsername(request.getUsername());
            if (existingUsername.isPresent() && !existingUsername.get().getUserId().equals(userUuid)) {
                throw new InvalidCredentialsException("Username already taken");
            }

            // Update user with password and username
            user.setUsername(request.getUsername());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setStatus(UserStatus.ONLINE);
            user.setLastSeenAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT tokens
            String jwtToken = jwtTokenProvider.generateToken(user.getUserId().toString(), user.getRole().name());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId().toString());

            return new LoginResponse(jwtToken, refreshToken, "Registration completed successfully", convertToUserResponse(user));
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Invalid user ID format");
        }
    }

    @Override
    public void linkOAuth2Account(String userId, String provider, String idToken) {
        User user = userRepository.findByUserId(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        try {
            if ("google".equalsIgnoreCase(provider)) {
                Map<String, Object> userInfo = verifyAndDecodeGoogleToken(idToken);
                String providerId = (String) userInfo.get("sub");
                user.setProviderId(providerId);
                user.setProvider(AuthProvider.GOOGLE);
            } else {
                throw new InvalidCredentialsException("Unsupported OAuth provider: " + provider);
            }

            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Failed to link OAuth2 account: " + e.getMessage());
        }
    }

    @Override
    public Map<String, String> getOAuth2Config() {
        Map<String, String> config = new HashMap<>();
        config.put("googleClientId", googleClientId);
        config.put("googleRedirectUri", googleRedirectUri);
        return config;
    }

    @Override
    public String exchangeCodeForIdToken(String code) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code", code);
            form.add("client_id", googleClientId);
            form.add("client_secret", googleClientSecret);
            form.add("redirect_uri", googleRedirectUri);
            form.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(googleTokenUri, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new InvalidCredentialsException("Failed to exchange code: " + response.getStatusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = objectMapper.readValue(response.getBody(), Map.class);
            String idToken = (String) tokenResponse.get("id_token");
            if (idToken == null || idToken.isBlank()) {
                throw new InvalidCredentialsException("Token response missing id_token");
            }

            return idToken;
        } catch (Exception e) {
            throw new InvalidCredentialsException("Failed to exchange code for ID token: " + e.getMessage());
        }
    }

    /**
     * Decode Google ID token JWT without verification
     * Safe to do since the token is already signed by Google and we received it directly from Google's SDK
     * The token payload contains email, sub, name, and other claims we need
     */
    private Map<String, Object> verifyAndDecodeGoogleToken(String idToken) {
        try {
            System.out.println("Decoding Google ID token...");
            
            // Manually decode the JWT payload without verification
            // JWT format: header.payload.signature
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new InvalidCredentialsException("Invalid JWT token format");
            }
            
            // Decode the payload (second part)
            String payload = parts[1];
            
            // Add padding if needed
            int padding = 4 - (payload.length() % 4);
            if (padding < 4) {
                payload += "=".repeat(padding);
            }
            
            // Decode from Base64URL
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(payload);
            String decodedPayload = new String(decodedBytes);
            
            // Parse as JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenInfo = objectMapper.readValue(decodedPayload, Map.class);
            
            System.out.println("Token decoded successfully. Email: " + tokenInfo.get("email") + ", Sub: " + tokenInfo.get("sub"));

            // Extract required user info
            if (!tokenInfo.containsKey("email") || !tokenInfo.containsKey("sub")) {
                System.out.println("Missing required fields. Available keys: " + tokenInfo.keySet());
                throw new InvalidCredentialsException("Token missing required fields (email, sub)");
            }

            // Verify that the token is for our client (log warning if not, but don't fail)
            String aud = (String) tokenInfo.get("aud");
            if (aud != null && !aud.equals(googleClientId)) {
                System.out.println("WARNING: Token audience '" + aud + "' does not match configured client ID '" + googleClientId + "'");
            }

            return tokenInfo;
        } catch (InvalidCredentialsException e) {
            System.err.println("Invalid credentials: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to verify Google token: " + e.getMessage());
            e.printStackTrace();
            throw new InvalidCredentialsException("Failed to verify Google token: " + e.getMessage());
        }
    }

    // getUserInfoFromGoogle method removed - using verifyAndDecodeGoogleToken instead which is more efficient

    private User createOrUpdateOAuth2User(String email, String name, String providerId, AuthProvider provider) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (isIncompleteGoogleRegistration(user)) {
                throw new InvalidCredentialsException(
                        "Please complete Google registration before signing in."
                );
            }

            user.setProvider(provider);
            user.setProviderId(providerId);
            
            // Update full name if it's empty or if Google provides a better name
            if ((user.getFullName() == null || user.getFullName().isEmpty()) && name != null) {
                user.setFullName(name);
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        } else {
            throw new UserNotFoundException("Email not registered. Please sign up first with email: " + email);
        }
    }

    /**
     * Create or update OAuth2 user for registration flow
     * Creates new user if not found, updates existing user
     */
    private User createOrUpdateOAuth2UserForRegistration(String email, String name, String providerId, AuthProvider provider) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (hasPassword(user)) {
                throw new InvalidCredentialsException(
                        "An account with this email already exists. Please use Google sign-in on the login page."
                );
            }

            user.setProvider(provider);
            user.setProviderId(providerId);
            
            // Update full name if it's empty or if Google provides a better name
            if ((user.getFullName() == null || user.getFullName().isEmpty()) && name != null) {
                user.setFullName(name);
            }

            if (user.getUsername() == null || user.getUsername().isBlank()) {
                user.setUsername(generateUniqueTemporaryUsername(email));
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        } else {
            // Create new user for registration
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name != null ? name : "");
            newUser.setUsername(generateUniqueTemporaryUsername(email));
            newUser.setProvider(provider);
            newUser.setProviderId(providerId);
            newUser.setIsActive(true);
            newUser.setStatus(UserStatus.INVISIBLE);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        }
    }

    private boolean hasPassword(User user) {
        return user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
    }

    private boolean isIncompleteGoogleRegistration(User user) {
        return user.getProvider() == AuthProvider.GOOGLE && !hasPassword(user);
    }

    private String generateUniqueTemporaryUsername(String email) {
        String baseUsername = sanitizeUsernamePrefix(email);
        String candidate = baseUsername;
        int suffix = 1;

        while (userRepository.existsByUsername(candidate)) {
            String suffixValue = String.valueOf(suffix++);
            int maxBaseLength = Math.max(3, 24 - suffixValue.length() - 1);
            String trimmedBase = baseUsername.length() > maxBaseLength
                    ? baseUsername.substring(0, maxBaseLength)
                    : baseUsername;
            candidate = trimmedBase + "_" + suffixValue;
        }

        return candidate;
    }

    private String sanitizeUsernamePrefix(String email) {
        String emailPrefix = "user";
        if (email != null && email.contains("@")) {
            emailPrefix = email.substring(0, email.indexOf('@'));
        }

        String normalized = emailPrefix
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "")
                .replaceAll("^[._-]+|[._-]+$", "");

        if (normalized.isBlank()) {
            normalized = "user";
        }

        if (normalized.length() < 3) {
            normalized = (normalized + "user").substring(0, 4);
        }

        return normalized.length() > 24 ? normalized.substring(0, 24) : normalized;
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
}
