package com.connecthub.auth.unit.service;

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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private UUID testUserId;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded_password_hash";
    private static final String VALID_TOKEN = "valid_jwt_token";
    private static final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        registerRequest = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD, "Test User");
        loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        
        testUser = new User(TEST_USERNAME, TEST_EMAIL, ENCODED_PASSWORD, AuthProvider.EMAIL);
        testUser.setUserId(testUserId);
    }

    // ===== REGISTRATION TESTS =====

    @Test
    @DisplayName("Should successfully register a new user")
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(testUserId.toString(), testUser.getRole().name())).thenReturn(VALID_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(testUserId.toString())).thenReturn("refresh_token");

        // Act
        LoginResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void testRegister_UsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should encode password using BCrypt during registration")
    void testRegister_PasswordEncoding() {
        // Arrange
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(anyString(), anyString())).thenReturn(VALID_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(anyString())).thenReturn("refresh_token");

        // Act
        authService.register(registerRequest);

        // Assert
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
    }

    // ===== LOGIN TESTS =====

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLogin_Success() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUserId.toString(), testUser.getRole().name())).thenReturn(VALID_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(testUserId.toString())).thenReturn("refresh_token");

        // Act
        LoginResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMessage());
        assertEquals(VALID_TOKEN, response.getToken());
        verify(userRepository, times(1)).findByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should throw exception when user not found during login")
    void testLogin_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    @DisplayName("Should throw exception when password is incorrect")
    void testLogin_InvalidPassword() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    @DisplayName("Should not call token generation on failed login")
    void testLogin_NoTokenOnFailure() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(loginRequest);
        });
        verify(jwtTokenProvider, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle login with special characters in password")
    void testLogin_SpecialCharactersInPassword() {
        // Arrange
        String specialPassword = "p@ssw0rd!#$%";
        LoginRequest specialLoginRequest = new LoginRequest(TEST_EMAIL, specialPassword);
        
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(specialPassword, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUserId.toString(), testUser.getRole().name())).thenReturn(VALID_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(testUserId.toString())).thenReturn("refresh_token");

        // Act
        LoginResponse response = authService.login(specialLoginRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getMessage());
    }

    // ===== TOKEN VALIDATION TESTS =====

    @Test
    @DisplayName("Should validate a valid token")
    void testValidateToken_Success() {
        // Arrange
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);

        // Act
        Boolean isValid = authService.validateToken(VALID_TOKEN);

        // Assert
        assertTrue(isValid);
        verify(jwtTokenProvider, times(1)).validateToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("Should reject an invalid token")
    void testValidateToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid_token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // Act
        Boolean isValid = authService.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for null token validation")
    void testValidateToken_NullToken() {
        // Arrange
        when(jwtTokenProvider.validateToken(null)).thenThrow(new IllegalArgumentException("Token cannot be null"));

        // Act & Assert
        assertFalse(authService.validateToken(null));
    }

    // ===== REFRESH TOKEN TESTS =====

    @Test
    @DisplayName("Should successfully refresh an expired token")
    void testRefreshToken_Success() {
        // Arrange
        String oldToken = "old_expired_token";
        String newToken = "new_fresh_token";
        
        when(jwtTokenProvider.validateToken(oldToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(oldToken)).thenReturn(testUserId.toString());
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(testUserId.toString(), testUser.getRole().name())).thenReturn(newToken);
        when(jwtTokenProvider.generateRefreshToken(testUserId.toString())).thenReturn("new_refresh_token");

        // Act
        LoginResponse response = authService.refreshToken(oldToken);

        // Assert
        assertNotNull(response);
        assertEquals(newToken, response.getToken());
    }

    @Test
    @DisplayName("Should fail to refresh invalid token")
    void testRefreshToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid_token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.refreshToken(invalidToken));
    }

    // ===== GET USER PROFILE TESTS =====

    @Test
    @DisplayName("Should retrieve user profile by ID")
    void testGetUserById_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        UserResponse response = authService.getUserById(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_EMAIL, response.getEmail());
        verify(userRepository, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when user profile not found")
    void testGetUserById_UserNotFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            authService.getUserById(testUserId);
        });
    }

    // ===== LOGOUT TESTS =====

    @Test
    @DisplayName("Should successfully logout user")
    void testLogout_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        authService.logout(testUserId);

        // Assert - Verify the method completes without exception
        // Additional assertions depend on implementation (token blacklist, etc.)
    }

    // ===== PASSWORD CHANGE TESTS =====

    @Test
    @DisplayName("Should successfully change user password")
    void testChangePassword_Success() {
        // Arrange
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword456";
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword(oldPassword);
        changePasswordRequest.setNewPassword(newPassword);
        changePasswordRequest.setConfirmPassword(newPassword);
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("new_encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.changePassword(testUserId, changePasswordRequest);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(newPassword);
    }

    @Test
    @DisplayName("Should reject password change with wrong old password")
    void testChangePassword_WrongOldPassword() {
        // Arrange
        String oldPassword = "wrongPassword";
        String newPassword = "newPassword456";
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword(oldPassword);
        changePasswordRequest.setNewPassword(newPassword);
        changePasswordRequest.setConfirmPassword(newPassword);
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, ENCODED_PASSWORD)).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.changePassword(testUserId, changePasswordRequest);
        });
    }

    // ===== USER SEARCH TESTS =====

    @Test
    @DisplayName("Should search users by query")
    void testSearchUsers_Success() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        when(userRepository.searchByQuery(TEST_USERNAME)).thenReturn(users);

        // Act
        List<UserResponse> results = authService.searchUsers(TEST_USERNAME);

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Should return empty list when no users match search")
    void testSearchUsers_NoResults() {
        // Arrange
        when(userRepository.searchByQuery("nonexistent")).thenReturn(Arrays.asList());

        // Act
        List<UserResponse> results = authService.searchUsers("nonexistent");

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ===== USER STATUS TESTS =====

    @Test
    @DisplayName("Should update user online status")
    void testUpdateStatus_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.updateStatus(testUserId, UserStatus.ONLINE);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ===== VERIFY PASSWORD TESTS =====

    @Test
    @DisplayName("Should verify password matches for user")
    void testVerifyPassword_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        // Act
        Boolean isVerified = authService.verifyPassword(testUserId, TEST_PASSWORD);

        // Assert
        assertTrue(isVerified);
    }

    @Test
    @DisplayName("Should reject incorrect password verification")
    void testVerifyPassword_Failure() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", ENCODED_PASSWORD)).thenReturn(false);

        // Act
        Boolean isVerified = authService.verifyPassword(testUserId, "wrongPassword");

        // Assert
        assertFalse(isVerified);
    }
}
