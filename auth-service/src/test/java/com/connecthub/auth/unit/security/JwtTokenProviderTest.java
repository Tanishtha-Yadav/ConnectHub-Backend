package com.connecthub.auth.unit.security;

import com.connecthub.auth.security.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String JWT_SECRET = "connecthubsecretkey123456789abcdefghijklmnopqrstuvwxyz";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpirationMs", 604800000L); // 7 days
    }

    // ===== ACCESS TOKEN GENERATION TESTS =====

    @Test
    @DisplayName("Should generate valid JWT token with user ID")
    void testGenerateToken_Success() {
        // Act
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Assert
        assertNotNull(token);
        assertNotEmpty(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void testGenerateToken_DifferentTokensForSameUser() throws InterruptedException {
        // Act
        String token1 = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");
        Thread.sleep(100); // Small delay to ensure different issuedAt times
        String token2 = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Assert
        assertNotEquals(token1, token2);
        assertTrue(jwtTokenProvider.validateToken(token1));
        assertTrue(jwtTokenProvider.validateToken(token2));
    }

    @Test
    @DisplayName("Should generate token with correct subject (user ID)")
    void testGenerateToken_CorrectSubject() {
        // Act
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Assert
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals(TEST_USER_ID, userId);
    }

    // ===== REFRESH TOKEN GENERATION TESTS =====

    @Test
    @DisplayName("Should generate refresh token with longer expiration")
    void testGenerateRefreshToken_Success() {
        // Act
        String refreshToken = jwtTokenProvider.generateRefreshToken(TEST_USER_ID);

        // Assert
        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertEquals(TEST_USER_ID, jwtTokenProvider.getUserIdFromToken(refreshToken));
    }

    @Test
    @DisplayName("Should generate different refresh tokens for same user")
    void testGenerateRefreshToken_DifferentTokens() throws InterruptedException {
        // Act
        String refreshToken1 = jwtTokenProvider.generateRefreshToken(TEST_USER_ID);
        Thread.sleep(100);
        String refreshToken2 = jwtTokenProvider.generateRefreshToken(TEST_USER_ID);

        // Assert
        assertNotEquals(refreshToken1, refreshToken2);
    }

    // ===== TOKEN VALIDATION TESTS =====

    @Test
    @DisplayName("Should validate a properly generated token")
    void testValidateToken_ValidToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Act
        Boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject null token")
    void testValidateToken_NullToken() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.validateToken(null);
        });
    }

    @Test
    @DisplayName("Should reject malformed JWT token")
    void testValidateToken_MalformedToken() {
        // Arrange
        String malformedToken = "invalid.token.format";

        // Act
        Boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject token with tampered signature")
    void testValidateToken_TamperedSignature() {
        // Arrange
        String validToken = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");
        String tamperedToken = validToken.substring(0, validToken.length() - 10) + "tamperedbits";

        // Act
        Boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void testValidateToken_InvalidSignature() {
        // Arrange - Generate token with different secret
        String tokenWithDifferentSecret = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTUxNjIzOTAyMn0." +
                "invalid_signature_here";

        // Act
        Boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentSecret);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject empty token string")
    void testValidateToken_EmptyToken() {
        // Act
        Boolean isValid = jwtTokenProvider.validateToken("");

        // Assert
        assertFalse(isValid);
    }

    // ===== EXTRACT USER ID TESTS =====

    @Test
    @DisplayName("Should extract correct user ID from token")
    void testGetUserIdFromToken_Success() {
        // Arrange
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Act
        String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertEquals(TEST_USER_ID, extractedUserId);
    }

    @Test
    @DisplayName("Should throw exception when extracting user ID from invalid token")
    void testGetUserIdFromToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.format";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getUserIdFromToken(invalidToken);
        });
    }

    @Test
    @DisplayName("Should throw exception when extracting user ID from expired token")
    void testGetUserIdFromToken_ExpiredToken() {
        // Arrange - Set expiration to past time
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000L);
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Reset expiration
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000L);

        // Act & Assert
        assertThrows(ExpiredJwtException.class, () -> {
            jwtTokenProvider.getUserIdFromToken(token);
        });
    }

    // ===== TOKEN EXPIRATION TESTS =====

    @Test
    @DisplayName("Should return false for non-expired token")
    void testIsTokenExpired_NotExpired() {
        // Arrange
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Act
        Boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should return true for expired token")
    void testIsTokenExpired_Expired() {
        // Arrange - Generate token with negative expiration
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000L);
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Act
        Boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Assert
        assertTrue(isExpired);
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Should handle multiple consecutive token validations")
    void testValidateToken_MultipleCalls() {
        // Arrange
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");

        // Act & Assert
        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Should handle whitespace in token string")
    void testValidateToken_TokenWithWhitespace() {
        // Arrange
        String token = jwtTokenProvider.generateToken(TEST_USER_ID, "ROLE_USER");
        String tokenWithWhitespace = " " + token + " ";

        // Act
        Boolean isValid = jwtTokenProvider.validateToken(tokenWithWhitespace);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should generate tokens with different user IDs correctly")
    void testGenerateToken_DifferentUsers() {
        // Arrange
        String userId1 = "550e8400-e29b-41d4-a716-446655440001";
        String userId2 = "550e8400-e29b-41d4-a716-446655440002";

        // Act
        String token1 = jwtTokenProvider.generateToken(userId1, "ROLE_USER");
        String token2 = jwtTokenProvider.generateToken(userId2, "ROLE_USER");

        // Assert
        assertEquals(userId1, jwtTokenProvider.getUserIdFromToken(token1));
        assertEquals(userId2, jwtTokenProvider.getUserIdFromToken(token2));
        assertNotEquals(token1, token2);
    }

    // ===== HELPER METHODS =====

    private void assertNotEmpty(String str) {
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }
}
