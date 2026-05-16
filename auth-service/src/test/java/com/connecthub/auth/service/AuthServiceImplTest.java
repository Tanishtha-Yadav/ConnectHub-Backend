package com.connecthub.auth.service;

import com.connecthub.auth.dto.request.*;
import com.connecthub.auth.dto.response.AuthResponse;
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
import com.connecthub.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User("testuser", "test@example.com", "hashedPassword", AuthProvider.EMAIL);
        testUser.setUserId(testUserId);
        testUser.setFullName("Test User");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFullName("New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        LoginResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should fail when email already exists")
    void testRegisterWithDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when username already exists")
    void testRegisterWithDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUserId.toString(), testUser.getRole().name())).thenReturn("validToken");
        when(jwtTokenProvider.generateRefreshToken(testUserId.toString())).thenReturn("validRefreshToken");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("validToken", response.getToken());
        assertEquals("validRefreshToken", response.getRefreshToken());
        assertEquals("Login successful", response.getMessage());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should fail login with invalid email")
    void testLoginWithInvalidEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should fail login with incorrect password")
    void testLoginWithIncorrectPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should successfully validate token")
    void testValidateTokenSuccess() {
        String token = "validToken";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);

        Boolean result = authService.validateToken(token);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void testValidateInvalidToken() {
        String token = "invalidToken";
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        Boolean result = authService.validateToken(token);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should get user ID from token")
    void testGetUserIdFromToken() {
        String token = "validToken";
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(testUserId.toString());

        UUID result = authService.getUserIdFromToken(token);

        assertEquals(testUserId, result);
    }

    @Test
    @DisplayName("Should successfully update user profile")
    void testUpdateProfileSuccess() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setAvatarUrl("https://example.com/avatar.jpg");
        request.setBio("Updated bio");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = authService.updateProfile(testUserId, request);

        assertNotNull(response);
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should fail to update profile for nonexistent user")
    void testUpdateProfileUserNotFound() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.updateProfile(testUserId, request));
    }

    @Test
    @DisplayName("Should successfully change password")
    void testChangePasswordSuccess() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword");
        request.setConfirmPassword("newpassword");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.changePassword(testUserId, request));
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should fail to change password with incorrect current password")
    void testChangePasswordWithIncorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongpassword");
        request.setNewPassword("newpassword");
        request.setConfirmPassword("newpassword");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(testUserId, request));
    }

    @Test
    @DisplayName("Should logout user successfully")
    void testLogoutSuccess() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.logout(testUserId));
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should update user status successfully")
    void testUpdateStatusSuccess() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = authService.updateStatus(testUserId, UserStatus.AWAY);

        assertNotNull(response);
        verify(userRepository, times(1)).save(any());
    }
}
