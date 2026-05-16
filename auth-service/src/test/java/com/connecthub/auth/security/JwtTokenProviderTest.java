package com.connecthub.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider
 */
@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=testSecretKey123456789abcdefghijklmnopqrstuvwxyz",
        "jwt.expiration=86400000",
        "jwt.refresh-expiration=604800000"
})
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken() {
        String token = jwtTokenProvider.generateToken(testUserId, "ROLE_USER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void testGenerateRefreshToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUserId);

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertTrue(refreshToken.contains("."));
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void testGetUserIdFromToken() {
        String token = jwtTokenProvider.generateToken(testUserId, "ROLE_USER");
        String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        assertEquals(testUserId, extractedUserId);
    }

    @Test
    @DisplayName("Should validate correct token")
    void testValidateValidToken() {
        String token = jwtTokenProvider.generateToken(testUserId, "ROLE_USER");
        Boolean isValid = jwtTokenProvider.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject invalid token signature")
    void testValidateInvalidTokenSignature() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.invalid_signature";
        Boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testValidateMalformedToken() {
        String malformedToken = "malformed.token";
        Boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should detect token expiration")
    void testIsTokenExpired() {
        String token = jwtTokenProvider.generateToken(testUserId, "ROLE_USER");
        Boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should successfully refresh token")
    void testRefreshToken() {
        String oldToken = jwtTokenProvider.generateToken(testUserId, "ROLE_USER");
        String newToken = jwtTokenProvider.generateRefreshToken(testUserId);

        assertNotEquals(oldToken, newToken);
        assertTrue(jwtTokenProvider.validateToken(newToken));
    }

    @Test
    @DisplayName("Should maintain user ID across token refresh")
    void testUserIdConsistencyAfterRefresh() {
        String token1 = jwtTokenProvider.generateToken(testUserId, "ROLE_USER");
        String userId1 = jwtTokenProvider.getUserIdFromToken(token1);

        String token2 = jwtTokenProvider.generateRefreshToken(testUserId);
        String userId2 = jwtTokenProvider.getUserIdFromToken(token2);

        assertEquals(userId1, userId2);
        assertEquals(testUserId, userId1);
        assertEquals(testUserId, userId2);
    }

    @Test
    @DisplayName("Should handle empty token gracefully")
    void testValidateEmptyToken() {
        Boolean isValid = jwtTokenProvider.validateToken("");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void testValidateNullToken() {
        assertThrows(IllegalArgumentException.class, () -> jwtTokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testGenerateDifferentTokensForDifferentUsers() {
        String userId2 = UUID.randomUUID().toString();
        String token1 = jwtTokenProvider.generateToken(testUserId, "ROLE_USER");
        String token2 = jwtTokenProvider.generateToken(userId2, "ROLE_USER");

        assertNotEquals(token1, token2);
        assertEquals(testUserId, jwtTokenProvider.getUserIdFromToken(token1));
        assertEquals(userId2, jwtTokenProvider.getUserIdFromToken(token2));
    }
}
