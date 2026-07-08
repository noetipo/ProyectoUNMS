package unmsm.edu.pe.security.infrastructure.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new PasswordEncoder();
    }

    @Test
    void encode_withPassword_returnsNonNullResult() {
        String encoded = passwordEncoder.encode("password123");
        assertNotNull(encoded);
        assertFalse(encoded.isBlank());
    }

    @Test
    void encode_doesNotReturnPlaintext() {
        String raw = "mySecret";
        assertNotEquals(raw, passwordEncoder.encode(raw));
    }

    @Test
    void encode_samePasswordTwice_returnsDifferentHashes() {
        String encoded1 = passwordEncoder.encode("password123");
        String encoded2 = passwordEncoder.encode("password123");
        assertNotEquals(encoded1, encoded2, "Each encoding should use a different salt");
    }

    @Test
    void matches_withCorrectPassword_returnsTrue() {
        String raw = "correctPassword";
        String encoded = passwordEncoder.encode(raw);
        assertTrue(passwordEncoder.matches(raw, encoded));
    }

    @Test
    void matches_withWrongPassword_returnsFalse() {
        String encoded = passwordEncoder.encode("correctPassword");
        assertFalse(passwordEncoder.matches("wrongPassword", encoded));
    }

    @Test
    void matches_withMultipleEncodings_eachVerifiesCorrectly() {
        String raw = "sharedSecret";
        String encoded1 = passwordEncoder.encode(raw);
        String encoded2 = passwordEncoder.encode(raw);
        assertTrue(passwordEncoder.matches(raw, encoded1));
        assertTrue(passwordEncoder.matches(raw, encoded2));
    }

    @Test
    void matches_withEmptyPassword_encodedCorrectly() {
        String encoded = passwordEncoder.encode("");
        assertTrue(passwordEncoder.matches("", encoded));
        assertFalse(passwordEncoder.matches("notempty", encoded));
    }
}