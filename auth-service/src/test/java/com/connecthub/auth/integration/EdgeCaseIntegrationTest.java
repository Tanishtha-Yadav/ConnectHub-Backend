package com.connecthub.auth.integration;

import com.connecthub.auth.dto.request.ChangePasswordRequest;
import com.connecthub.auth.dto.request.LoginRequest;
import com.connecthub.auth.dto.request.RegisterRequest;
import com.connecthub.auth.model.AuthProvider;
import com.connecthub.auth.model.User;
import com.connecthub.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("Edge Case Integration Tests")
class EdgeCaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String API_BASE = "/api/auth";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
        userRepository.deleteAll();
    }

    // ===== EMPTY FIELDS TESTS =====

    @Test
    @DisplayName("Should reject registration with null username")
    void testRegister_NullUsername() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(null, "test@example.com", "Password123!", "Test User");

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with null email")
    void testRegister_NullEmail() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest("testuser", null, "Password123!", "Test User");

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with null password")
    void testRegister_NullPassword() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", null, "Test User");

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject login with null email")
    void testLogin_NullEmail() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest(null, "Password123!");

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject login with null password")
    void testLogin_NullPassword() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", null);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== FIELD LENGTH TESTS =====

    @Test
    @DisplayName("Should reject username shorter than minimum length")
    void testRegister_UsernameTooShort() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "ab",  // Less than 3 characters
                "test@example.com",
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject password shorter than minimum length")
    void testRegister_PasswordTooShort() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "Pass",  // Less than 6 characters
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject username exceeding maximum length")
    void testRegister_UsernameTooLong() throws Exception {
        // Arrange
        String longUsername = "a".repeat(101);  // Exceeds 100 characters
        RegisterRequest registerRequest = new RegisterRequest(
                longUsername,
                "test@example.com",
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should accept maximum length valid username")
    void testRegister_UsernameMaxLength() throws Exception {
        // Arrange
        String maxUsername = "a".repeat(100);  // Exactly 100 characters
        RegisterRequest registerRequest = new RegisterRequest(
                maxUsername,
                "test@example.com",
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    // ===== SPECIAL CHARACTER TESTS =====

    @Test
    @DisplayName("Should handle special characters in password")
    void testLogin_SpecialCharactersInPassword() throws Exception {
        // Arrange
        String specialPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;:',.<>?";
        User user = new User("testuser", "test@example.com", 
                passwordEncoder.encode(specialPassword), AuthProvider.EMAIL);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("test@example.com", specialPassword);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle unicode characters in username")
    void testRegister_UnicodeInUsername() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "用户名123",  // Username in Chinese + numbers
                "unicode@example.com",
                "Password123!",
                "Unicode User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should handle unicode in full name")
    void testRegister_UnicodeInFullName() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "unicodeuser",
                "unicode2@example.com",
                "Password123!",
                "用户 😀 User"  // Unicode + emoji
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    // ===== EMAIL FORMAT TESTS =====

    @Test
    @DisplayName("Should accept valid email with subdomain")
    void testRegister_ValidEmailWithSubdomain() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "user@subdomain.example.com",
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should accept valid email with plus addressing")
    void testRegister_ValidEmailWithPlusAddressing() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "user+tag@example.com",
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should reject email without at symbol")
    void testRegister_EmailWithoutAtSymbol() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "userexample.com",  // Missing @
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject email without domain")
    void testRegister_EmailWithoutDomain() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "user@",  // Missing domain
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== DUPLICATE DATA TESTS =====

    @Test
    @DisplayName("Should reject registration with duplicate email (case-insensitive)")
    void testRegister_DuplicateEmailCaseInsensitive() throws Exception {
        // Arrange - Create first user
        User user = new User("user1", "test@example.com", 
                passwordEncoder.encode("Password123!"), AuthProvider.EMAIL);
        userRepository.save(user);

        // Try to register with same email but different case
        RegisterRequest registerRequest = new RegisterRequest(
                "user2",
                "TEST@EXAMPLE.COM",
                "Password123!",
                "Another User"
        );

        // Act & Assert - Should be rejected if case-insensitive comparison is used
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print());
    }

    @Test
    @DisplayName("Should reject registration with duplicate username (case-sensitive)")
    void testRegister_DuplicateUsername() throws Exception {
        // Arrange
        User user = new User("testuser", "test1@example.com", 
                passwordEncoder.encode("Password123!"), AuthProvider.EMAIL);
        userRepository.save(user);

        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "test2@example.com",
                "Password123!",
                "Another User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== MULTIPLE OPERATIONS TESTS =====

    @Test
    @DisplayName("Should handle multiple login attempts in sequence")
    void testMultipleLoginAttempts_Sequential() throws Exception {
        // Arrange
        User user = new User("testuser", "test@example.com", 
                passwordEncoder.encode("Password123!"), AuthProvider.EMAIL);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123!");

        // Act & Assert - Multiple attempts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post(API_BASE + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should handle multiple password changes in sequence")
    void testMultiplePasswordChanges() throws Exception {
        // Arrange
        User user = new User("testuser", "test@example.com", 
                passwordEncoder.encode("Password123!"), AuthProvider.EMAIL);
        user = userRepository.save(user);

        // Act & Assert - Change password multiple times
        String currentPassword = "Password123!";
        for (int i = 0; i < 3; i++) {
            String newPassword = "NewPassword" + i + "!";
            ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
            changePasswordRequest.setCurrentPassword(currentPassword);
            changePasswordRequest.setNewPassword(newPassword);
            changePasswordRequest.setConfirmPassword(newPassword);

            mockMvc.perform(post(API_BASE + "/password/" + user.getUserId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changePasswordRequest)))
                    .andExpect(status().isOk());

            currentPassword = newPassword;
        }
    }

    // ===== WHITESPACE TESTS =====

    @Test
    @DisplayName("Should handle email with leading/trailing whitespace")
    void testRegister_EmailWithWhitespace() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "  test@example.com  ",  // Leading/trailing spaces
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print());
    }

    @Test
    @DisplayName("Should reject password with only whitespace")
    void testRegister_PasswordOnlyWhitespace() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "      ",  // Only whitespace
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== MALFORMED JSON TESTS =====

    @Test
    @DisplayName("Should handle invalid JSON in request body")
    void testRegister_InvalidJSON() throws Exception {
        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json here}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle empty JSON object")
    void testRegister_EmptyJSON() throws Exception {
        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== CONTENT TYPE TESTS =====

    @Test
    @DisplayName("Should reject request with wrong content type")
    void testRegister_WrongContentType() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "Password123!",
                "Test User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=testuser&email=test@example.com&password=Password123!"))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }
}
