package com.connecthub.auth.integration;

import com.connecthub.auth.dto.request.ChangePasswordRequest;
import com.connecthub.auth.dto.request.LoginRequest;
import com.connecthub.auth.dto.request.RegisterRequest;
import com.connecthub.auth.dto.request.UpdateProfileRequest;
import com.connecthub.auth.dto.response.LoginResponse;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

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
    private static final String TEST_EMAIL = "integration@test.com";
    private static final String TEST_USERNAME = "integrationuser";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_FULL_NAME = "Integration Test User";

    @BeforeEach
    void setUp() {
        // Initialize MockMvc using WebApplicationContext
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
        // Clean database before each test
        userRepository.deleteAll();
    }

    // ===== REGISTRATION INTEGRATION TESTS =====

    @Test
    @DisplayName("Should register new user and return 201 Created")
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                TEST_USERNAME,
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_FULL_NAME
        );

        // Act & Assert
        MvcResult result = mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        // Verify user was created in database
        assertThat(userRepository.findByEmail(TEST_EMAIL)).isPresent();
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void testRegister_DuplicateEmail() throws Exception {
        // Arrange - Create first user
        User existingUser = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        userRepository.save(existingUser);

        RegisterRequest registerRequest = new RegisterRequest(
                "anotheruser",
                TEST_EMAIL,
                TEST_PASSWORD,
                "Another User"
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should reject registration with invalid email format")
    void testRegister_InvalidEmail() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                TEST_USERNAME,
                "invalid-email",
                TEST_PASSWORD,
                TEST_FULL_NAME
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with weak password")
    void testRegister_WeakPassword() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                TEST_USERNAME,
                TEST_EMAIL,
                "short",  // Too short
                TEST_FULL_NAME
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with empty fields")
    void testRegister_EmptyFields() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "",
                "",
                "",
                ""
        );

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== LOGIN INTEGRATION TESTS =====

    @Test
    @DisplayName("Should login successfully with valid credentials and return JWT token")
    void testLogin_Success() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        // Act & Assert
        MvcResult result = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        // Extract token
        String response = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        assertThat(loginResponse.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should reject login with non-existent user")
    void testLogin_UserNotFound() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should reject login with incorrect password")
    void testLogin_WrongPassword() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, "WrongPassword123!");

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should reject login with missing email")
    void testLogin_MissingEmail() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("", TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject login with missing password")
    void testLogin_MissingPassword() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, "");

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ===== TOKEN VALIDATION TESTS =====

    @Test
    @DisplayName("Should validate a valid token")
    void testValidateToken_Success() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        user = userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        String token = loginResponse.getToken();

        // Act & Assert
        mockMvc.perform(get(API_BASE + "/validate/" + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("Should reject invalid token")
    void testValidateToken_InvalidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get(API_BASE + "/validate/invalid_token_here"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ===== REFRESH TOKEN TESTS =====

    @Test
    @DisplayName("Should refresh token and return new JWT")
    void testRefreshToken_Success() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        userRepository.save(user);

        // First login
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        String oldToken = loginResponse.getToken();

        // Act & Assert - Refresh the token
        MvcResult refreshResult = mockMvc.perform(post(API_BASE + "/refresh")
                .header("Authorization", "Bearer " + oldToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String refreshResponse = refreshResult.getResponse().getContentAsString();
        LoginResponse newLoginResponse = objectMapper.readValue(refreshResponse, LoginResponse.class);
        assertThat(newLoginResponse.getToken()).isNotBlank();
    }

    // ===== PROTECTED ENDPOINT TESTS =====

    @Test
    @DisplayName("Should access protected endpoint with valid token")
    void testGetProfile_WithValidToken() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        user = userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        String token = loginResponse.getToken();

        // Act & Assert
        mockMvc.perform(get(API_BASE + "/profile/" + user.getUserId())
                .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    @DisplayName("Should reject access to protected endpoint without token")
    void testGetProfile_WithoutToken() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        user = userRepository.save(user);

        // Act & Assert
        mockMvc.perform(get(API_BASE + "/profile/" + user.getUserId()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject access with invalid token")
    void testGetProfile_WithInvalidToken() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        user = userRepository.save(user);

        // Act & Assert
        mockMvc.perform(get(API_BASE + "/profile/" + user.getUserId())
                .header("Authorization", "Bearer invalid_token"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // ===== LOGOUT TESTS =====

    @Test
    @DisplayName("Should logout user successfully")
    void testLogout_Success() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        user = userRepository.save(user);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/logout/" + user.getUserId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    // ===== PASSWORD CHANGE TESTS =====

    @Test
    @DisplayName("Should change password successfully")
    void testChangePassword_Success() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        user = userRepository.save(user);

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword(TEST_PASSWORD);
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/password/" + user.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("Should reject password change with wrong old password")
    void testChangePassword_WrongOldPassword() throws Exception {
        // Arrange
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        user = userRepository.save(user);

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("WrongOldPassword");
        changePasswordRequest.setNewPassword("NewPassword123!");
        changePasswordRequest.setConfirmPassword("NewPassword123!");

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/password/" + user.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // ===== FULL LOGIN FLOW TEST =====

    @Test
    @DisplayName("Should complete full authentication flow: register -> login -> access protected resource")
    void testFullAuthenticationFlow() throws Exception {
        // Step 1: Register
        RegisterRequest registerRequest = new RegisterRequest(
                TEST_USERNAME,
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_FULL_NAME
        );

        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Step 2: Login
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        String token = loginResponse.getToken();

        // Step 3: Access protected resource
        User user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        mockMvc.perform(get(API_BASE + "/profile/" + user.getUserId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }
}
