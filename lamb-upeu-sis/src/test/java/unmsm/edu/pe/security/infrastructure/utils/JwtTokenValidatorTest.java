package unmsm.edu.pe.security.infrastructure.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenValidatorTest {

    private JwtTokenValidator jwtTokenValidator;
    private static final String SECRET_KEY = "mySecretKey1234567890abcdefghij";

    @BeforeEach
    void setUp() {
        jwtTokenValidator = new JwtTokenValidator();
    }

    private String buildToken(String subject, String userId, String role, long expiresInSeconds) throws Exception {
        long exp = Instant.now().plusSeconds(expiresInSeconds).getEpochSecond();
        long iat = Instant.now().getEpochSecond();

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format(
                "{\"iss\":\"https://unmsm.edu.pe\",\"aud\":\"upeu-sis\"," +
                "\"sub\":\"%s\",\"userId\":\"%s\",\"roles\":[\"%s\"],\"exp\":%d,\"iat\":%d}",
                subject, userId, role, exp, iat
        );

        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String data = encodedHeader + "." + encodedPayload;
        String signature = hmacSha256(data, SECRET_KEY);
        return data + "." + signature;
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    // --- validateToken() ---

    @Test
    void validateToken_withValidToken_returnsTrue() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        assertTrue(jwtTokenValidator.validateToken(token));
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", -10);
        assertFalse(jwtTokenValidator.validateToken(token));
    }

    @Test
    void validateToken_withNull_returnsFalse() {
        assertFalse(jwtTokenValidator.validateToken(null));
    }

    @Test
    void validateToken_withEmptyString_returnsFalse() {
        assertFalse(jwtTokenValidator.validateToken(""));
    }

    @Test
    void validateToken_withBlankString_returnsFalse() {
        assertFalse(jwtTokenValidator.validateToken("   "));
    }

    @Test
    void validateToken_withInvalidStructure_returnsFalse() {
        assertFalse(jwtTokenValidator.validateToken("not.a.valid.jwt.with.too.many.parts"));
    }

    @Test
    void validateToken_withTamperedSignature_returnsFalse() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "tampered_INVALID";
        assertFalse(jwtTokenValidator.validateToken(tampered));
    }

    @Test
    void validateToken_withBearerPrefix_returnsTrue() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        assertTrue(jwtTokenValidator.validateToken("Bearer " + token));
    }

    // --- getUsernameFromToken() ---

    @Test
    void getUsernameFromToken_withValidToken_returnsSubject() throws Exception {
        String token = buildToken("john_doe", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        assertEquals("john_doe", jwtTokenValidator.getUsernameFromToken(token));
    }

    @Test
    void getUsernameFromToken_withBearerPrefix_returnsSubject() throws Exception {
        String token = buildToken("john_doe", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        assertEquals("john_doe", jwtTokenValidator.getUsernameFromToken("Bearer " + token));
    }

    // --- getUserIdFromToken() ---

    @Test
    void getUserIdFromToken_withValidToken_returnsUserId() throws Exception {
        String expectedId = "550e8400-e29b-41d4-a716-446655440000";
        String token = buildToken("testuser", expectedId, "USER", 3600);
        assertEquals(expectedId, jwtTokenValidator.getUserIdFromToken(token));
    }

    // --- getUserIdAsUUID() ---

    @Test
    void getUserIdAsUUID_withValidToken_returnsUUID() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        UUID uuid = jwtTokenValidator.getUserIdAsUUID(token);
        assertNotNull(uuid);
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), uuid);
    }

    // --- getRolesFromToken() ---

    @Test
    void getRolesFromToken_withAdminRole_returnsAdmin() throws Exception {
        String token = buildToken("adminuser", "550e8400-e29b-41d4-a716-446655440000", "ADMIN", 3600);
        assertEquals(List.of("ADMIN"), jwtTokenValidator.getRolesFromToken(token));
    }

    @Test
    void getRolesFromToken_withUserRole_returnsUser() throws Exception {
        String token = buildToken("normaluser", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        assertEquals(List.of("USER"), jwtTokenValidator.getRolesFromToken(token));
    }

    @Test
    void getRolesFromToken_withNoRolesClaim_returnsEmptyList() {
        assertTrue(jwtTokenValidator.getRolesFromToken("not-a-token").isEmpty());
    }

    // --- isTokenExpired() ---

    @Test
    void isTokenExpired_withFutureExpiry_returnsFalse() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        assertFalse(jwtTokenValidator.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_withPastExpiry_returnsTrue() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", -60);
        assertTrue(jwtTokenValidator.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_withInvalidToken_returnsTrue() {
        assertTrue(jwtTokenValidator.isTokenExpired("not-a-token"));
    }

    // --- getClaimAsString() ---

    @Test
    void getClaimAsString_withExistingClaim_returnsClaim() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        assertEquals("testuser", jwtTokenValidator.getClaimAsString(token, "sub"));
        assertEquals("https://unmsm.edu.pe", jwtTokenValidator.getClaimAsString(token, "iss"));
    }

    @Test
    void getClaimAsString_withNonExistentClaim_returnsNull() throws Exception {
        String token = buildToken("testuser", "550e8400-e29b-41d4-a716-446655440000", "USER", 3600);
        assertNull(jwtTokenValidator.getClaimAsString(token, "nonexistent"));
    }
}