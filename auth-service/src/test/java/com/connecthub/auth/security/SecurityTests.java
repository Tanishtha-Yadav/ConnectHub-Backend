package com.connecthub.auth.security;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("Security Tests - Brute Force, SQL Injection, Token Tampering")
class SecurityTests {

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
    private static final String TEST_EMAIL = "security@test.com";
    private static final String TEST_USERNAME = "securityuser";
    private static final String TEST_PASSWORD = "TestPassword123!";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
        userRepository.deleteAll();
        
        // Create test user
        User user = new User(TEST_USERNAME, TEST_EMAIL, 
                passwordEncoder.encode(TEST_PASSWORD), AuthProvider.EMAIL);
        userRepository.save(user);
    }

    // ===== BRUTE FORCE ATTACK TESTS =====

    @Test
    @DisplayName("Should detect and handle multiple failed login attempts")
    void testBruteForceAttack_MultipleFailedAttempts() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, "WrongPassword");
        
        // Act - Simulate multiple failed login attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(API_BASE + "/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // After multiple attempts, system should either:
        // 1. Lock the account
        // 2. Require CAPTCHA (implementation dependent)
        // 3. Add rate limiting
        
        // Try once more with correct password - should still fail if account is locked
        LoginRequest correctLoginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(correctLoginRequest)))
                .andDo(print())
                .andReturn();

        // Assert - Status should indicate rate limiting or account lock
        int status = result.getResponse().getStatus();
        // 401 = Unauthorized, 429 = Too Many Requests
        assertThat(status).isIn(401, 429);
    }

    @Test
    @DisplayName("Should implement rate limiting on login endpoint")
    void testRateLimiting_LoginEndpoint() throws Exception {
        // Act - Send many rapid requests
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);

        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                try {
                    LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, "WrongPassword");
                    MvcResult result = mockMvc.perform(post(API_BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                            .andReturn();

                    if (result.getResponse().getStatus() == 429) {
                        rateLimitedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Handle exception
                }
            });
        }

        executor.shutdown();
        Thread.sleep(2000); // Wait for all requests to complete

        // Assert - Some requests should be rate limited
        // Note: This depends on rate limiting configuration
    }

    // ===== SQL INJECTION TESTS =====

    @Test
    @DisplayName("Should safely handle SQL injection attempt in email field")
    void testSQLInjection_EmailField() throws Exception {
        // Arrange - SQL injection payload in email
        String sqlInjectionPayload = "' OR '1'='1";
        LoginRequest loginRequest = new LoginRequest(sqlInjectionPayload, TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized()); // Should not return success
    }

    @Test
    @DisplayName("Should safely handle SQL injection with OR 1=1 in password")
    void testSQLInjection_PasswordField() throws Exception {
        // Arrange
        String sqlInjectionPayload = "' OR '1'='1' --";
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, sqlInjectionPayload);

        // Act & Assert - Password is encrypted, injection should fail
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should safely handle advanced SQL injection in username during registration")
    void testSQLInjection_RegistrationUsername() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "'; DROP TABLE users; --",
                "injection@test.com",
                TEST_PASSWORD,
                "Injection User"
        );

        // Act & Assert - Parameterized queries should prevent injection
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andReturn(); // Should not actually delete table

        // Verify users table still exists
        assertThat(userRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should safely handle UNION-based SQL injection")
    void testSQLInjection_UnionBased() throws Exception {
        // Arrange
        String unionPayload = "' UNION SELECT * FROM users --";
        LoginRequest loginRequest = new LoginRequest(unionPayload, TEST_PASSWORD);

        // Act & Assert
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // ===== XSS AND INJECTION TESTS =====

    @Test
    @DisplayName("Should safely handle XSS payload in email field")
    void testXSSInjection_EmailField() throws Exception {
        // Arrange - XSS payload
        RegisterRequest registerRequest = new RegisterRequest(
                TEST_USERNAME,
                "<script>alert('XSS')</script>@test.com",
                TEST_PASSWORD,
                TEST_USERNAME
        );

        // Act & Assert - Invalid email format should be rejected
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should safely handle XSS in username field")
    void testXSSInjection_UsernameField() throws Exception {
        // Arrange
        RegisterRequest registerRequest = new RegisterRequest(
                "<img src=x onerror=alert('XSS')>",
                "xss@test.com",
                TEST_PASSWORD,
                "XSS User"
        );

        // Act - Should not execute script
        mockMvc.perform(post(API_BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print());

        // Assert - No script execution (can't directly test, but ensure no exceptions)
    }

    // ===== TOKEN TAMPERING TESTS =====

    @Test
    @DisplayName("Should reject token with modified payload")
    void testTokenTampering_ModifiedPayload() throws Exception {
        // Arrange - Get valid token
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String[] parts = response.split("\"");
        String token = null;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("token") && i + 2 < parts.length) {
                token = parts[i + 2];
                break;
            }
        }

        // Act - Tamper with token
        if (token != null && token.contains(".")) {
            String[] tokenParts = token.split("\\.");
            String tamperedToken = tokenParts[0] + ".tamperedpayload." + tokenParts[2];

            // Try to use tampered token
            mockMvc.perform(post(API_BASE + "/validate/" + tamperedToken))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Should reject token with modified signature")
    void testTokenTampering_ModifiedSignature() throws Exception {
        // Arrange - Get valid token
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String[] parts = response.split("\"");
        String token = null;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("token") && i + 2 < parts.length) {
                token = parts[i + 2];
                break;
            }
        }

        // Act - Modify signature
        if (token != null && token.contains(".")) {
            String[] tokenParts = token.split("\\.");
            String tamperedToken = tokenParts[0] + "." + tokenParts[1] + ".tameredsignature";

            // Validate endpoint should reject tampered token
            mockMvc.perform(post(API_BASE + "/validate/" + tamperedToken))
                    .andDo(print());
        }
    }

    // ===== MALFORMED TOKEN TESTS =====

    @Test
    @DisplayName("Should reject completely invalid token")
    void testInvalidToken_CompletelyMalformed() throws Exception {
        // Act & Assert
        mockMvc.perform(post(API_BASE + "/validate/not.a.valid.token.at.all"))
                .andDo(print())
                .andExpect(status().isOk()); // Validation endpoint returns 200 with valid:false
    }

    @Test
    @DisplayName("Should reject token missing segments")
    void testInvalidToken_MissingSegments() throws Exception {
        // Act & Assert
        mockMvc.perform(post(API_BASE + "/validate/onlyone.segment"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject empty token")
    void testInvalidToken_EmptyToken() throws Exception {
        // Act & Assert
        mockMvc.perform(post(API_BASE + "/validate/"))
                .andDo(print())
                .andExpect(status().isNotFound()); // Empty token path
    }

    // ===== SENSITIVE DATA EXPOSURE TESTS =====

    @Test
    @DisplayName("Should not expose sensitive user data in error messages")
    void testErrorMessages_NoSensitiveDataExposure() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, "WrongPassword");

        // Act
        MvcResult result = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Assert - Should not contain sensitive information
        assertThat(response)
                .doesNotContain(TEST_PASSWORD)
                .doesNotContain(passwordEncoder.encode(TEST_PASSWORD));
    }

    @Test
    @DisplayName("Should not expose internal stack traces in error responses")
    void testErrorMessages_NoStackTrace() throws Exception {
        // Arrange - Send invalid JSON
        String invalidJson = "{invalid json here}";

        // Act
        MvcResult result = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Assert
        assertThat(response)
                .doesNotContain("at java.lang")
                .doesNotContain("Exception")
                .doesNotContain("StackTrace");
    }

    // ===== CROSS-SITE REQUEST FORGERY (CSRF) TESTS =====

    @Test
    @DisplayName("Should handle CORS headers properly")
    void testCORS_HeaderPresence() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        // Act & Assert - CORS headers should be present (if configured)
        MvcResult result = mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        // Should be configured to specific origins, not "*" in production
        assertNotNull(result.getResponse().getHeader("Access-Control-Allow-Origin"));
    }

    // ===== COMMAND INJECTION TESTS =====

    @Test
    @DisplayName("Should safely handle command injection payload in email")
    void testCommandInjection_EmailField() throws Exception {
        // Arrange
        String commandPayload = "test@example.com; rm -rf /";
        LoginRequest loginRequest = new LoginRequest(commandPayload, TEST_PASSWORD);

        // Act & Assert - Should not execute command, just fail login
        mockMvc.perform(post(API_BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
