package com.connecthub.auth.unit.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Password Encoder Unit Tests")
class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;
    private static final String TEST_PASSWORD = "SecurePass123!@#";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
    }

    // ===== PASSWORD ENCODING TESTS =====

    @Test
    @DisplayName("Should encode password successfully")
    void testEncodePassword_Success() {
        // Act
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        // Assert
        assertNotNull(encodedPassword);
        assertNotEquals(TEST_PASSWORD, encodedPassword);
        assertTrue(encodedPassword.length() > TEST_PASSWORD.length());
    }

    @Test
    @DisplayName("Should produce different hashes for same password (due to salt)")
    void testEncodePassword_DifferentHashesForSamePassword() {
        // Act
        String hash1 = passwordEncoder.encode(TEST_PASSWORD);
        String hash2 = passwordEncoder.encode(TEST_PASSWORD);

        // Assert
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should encode empty password")
    void testEncodePassword_EmptyPassword() {
        // Act
        String encodedPassword = passwordEncoder.encode("");

        // Assert
        assertNotNull(encodedPassword);
        assertNotEmpty(encodedPassword);
    }

    @Test
    @DisplayName("Should encode password with special characters")
    void testEncodePassword_SpecialCharacters() {
        // Arrange
        String specialPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;:',.<>?/~`";

        // Act
        String encodedPassword = passwordEncoder.encode(specialPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(specialPassword, encodedPassword));
    }

    @Test
    @DisplayName("Should encode very long password")
    void testEncodePassword_VeryLongPassword() {
        // Arrange
        String longPassword = "a".repeat(1000) + "P@ssw0rd!123";

        // Act
        String encodedPassword = passwordEncoder.encode(longPassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(longPassword, encodedPassword));
    }

    @Test
    @DisplayName("Should encode password with unicode characters")
    void testEncodePassword_UnicodeCharacters() {
        // Arrange
        String unicodePassword = "P@ssw0rd! 你好世界 🔐";

        // Act
        String encodedPassword = passwordEncoder.encode(unicodePassword);

        // Assert
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(unicodePassword, encodedPassword));
    }

    // ===== PASSWORD MATCHING TESTS =====

    @Test
    @DisplayName("Should match correct password against encoded hash")
    void testMatchPassword_CorrectPassword() {
        // Arrange
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        // Act
        boolean matches = passwordEncoder.matches(TEST_PASSWORD, encodedPassword);

        // Assert
        assertTrue(matches);
    }

    @Test
    @DisplayName("Should not match incorrect password against encoded hash")
    void testMatchPassword_IncorrectPassword() {
        // Arrange
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        String wrongPassword = "WrongPassword123!@#";

        // Act
        boolean matches = passwordEncoder.matches(wrongPassword, encodedPassword);

        // Assert
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should not match empty password against non-empty hash")
    void testMatchPassword_EmptyPassword() {
        // Arrange
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        // Act
        boolean matches = passwordEncoder.matches("", encodedPassword);

        // Assert
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should handle null password in matching gracefully")
    void testMatchPassword_NullPassword() {
        // Arrange
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            passwordEncoder.matches(null, encodedPassword);
        });
    }

    @Test
    @DisplayName("Should be case-sensitive when matching passwords")
    void testMatchPassword_CaseSensitive() {
        // Arrange
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        String differentCasePassword = TEST_PASSWORD.toLowerCase();

        // Act
        boolean matches = passwordEncoder.matches(differentCasePassword, encodedPassword);

        // Assert
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should be whitespace-sensitive")
    void testMatchPassword_WhitespaceSensitive() {
        // Arrange
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        String passwordWithExtra = " " + TEST_PASSWORD + " ";

        // Act
        boolean matches = passwordEncoder.matches(passwordWithExtra, encodedPassword);

        // Assert
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should match password with special characters")
    void testMatchPassword_SpecialCharacters() {
        // Arrange
        String specialPassword = "P@ssw0rd!#$%^&*()";
        String encodedPassword = passwordEncoder.encode(specialPassword);

        // Act
        boolean matches = passwordEncoder.matches(specialPassword, encodedPassword);

        // Assert
        assertTrue(matches);
    }

    // ===== BCRYPT STRENGTH TESTS =====

    @Test
    @DisplayName("Should use proper BCrypt strength")
    void testBCryptStrength() {
        // Arrange
        String encoded = passwordEncoder.encode(TEST_PASSWORD);

        // Assert - BCrypt hash should start with $2a$, $2b$, or $2x$ and contain strength indicator
        assertTrue(encoded.startsWith("$2"));
        assertTrue(encoded.length() >= 60);
        assertNotNull(encoded);
    }

    // ===== PASSWORD SECURITY TESTS =====

    @Test
    @DisplayName("Should not be reversible (one-way function)")
    void testPasswordNotReversible() {
        // Arrange
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        // Assert - Cannot decode back to original
        assertNotEquals(TEST_PASSWORD, encodedPassword);
        // Even with perfect algorithm, BCrypt is one-way
    }

    @Test
    @DisplayName("Should prevent rainbow table attacks")
    void testRainbowTableProtection() {
        // Arrange
        String hash1 = passwordEncoder.encode(TEST_PASSWORD);
        String hash2 = passwordEncoder.encode(TEST_PASSWORD);

        // Assert - Different salts produce different hashes
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should have computational delay (BCrypt iteration)")
    void testBCryptComputationalCost() {
        // Arrange
        long startTime = System.currentTimeMillis();

        // Act - Encode password
        passwordEncoder.encode(TEST_PASSWORD);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert - Should take noticeable time (at least 10ms with strength 10)
        assertTrue(duration > 10, "BCrypt should have computational cost");
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Should encode null hash safely")
    void testMatchPassword_NullHash() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            passwordEncoder.matches(TEST_PASSWORD, null);
        });
    }

    @Test
    @DisplayName("Should handle very short password")
    void testEncodePassword_VeryShortPassword() {
        // Arrange
        String shortPassword = "a";

        // Act
        String encoded = passwordEncoder.encode(shortPassword);

        // Assert
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches(shortPassword, encoded));
    }

    @Test
    @DisplayName("Should handle password with leading/trailing special chars")
    void testEncodePassword_LeadingTrailingSpecialChars() {
        // Arrange
        String password = "!@#$%TestPass123!@#$%";

        // Act
        String encoded = passwordEncoder.encode(password);

        // Assert
        assertTrue(passwordEncoder.matches(password, encoded));
    }

    // ===== HELPER METHOD =====
    private void assertNotEmpty(String str) {
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }
}
