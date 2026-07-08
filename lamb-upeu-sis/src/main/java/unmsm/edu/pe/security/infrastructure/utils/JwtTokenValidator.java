package unmsm.edu.pe.security.infrastructure.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JwtTokenValidator {

    // ✅ Usar la misma clave que JwtTokenGenerator
    // Idealmente debería venir de configuración
    private static final String SECRET_KEY = "mySecretKey1234567890abcdefghij";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean validateToken(String token) {
        System.out.println("\n=== JWT VALIDATOR DEBUG ===");

        try {
            if (token == null || token.trim().isEmpty()) {
                System.out.println("❌ Token is null or empty");
                return false;
            }

            // Remover "Bearer " si está presente
            String cleanToken = token;
            if (cleanToken.startsWith("Bearer ")) {
                cleanToken = cleanToken.substring(7);
                System.out.println("✓ Removed 'Bearer ' prefix");
            }

            String[] parts = cleanToken.split("\\.");
            System.out.println("Token parts count: " + parts.length);

            if (parts.length != 3) {
                System.out.println("❌ Invalid token structure (expected 3 parts, got " + parts.length + ")");
                return false;
            }

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            System.out.println("Header (encoded): " + header.substring(0, Math.min(20, header.length())) + "...");
            System.out.println("Payload (encoded): " + payload.substring(0, Math.min(30, payload.length())) + "...");
            System.out.println("Signature (encoded): " + signature.substring(0, Math.min(20, signature.length())) + "...");

            // ✅ Decodificar y mostrar payload para debug
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            System.out.println("Decoded payload: " + decodedPayload);

            // ✅ Verificar la firma
            String data = header + "." + payload;
            String expectedSignature = hmacSha256(data, SECRET_KEY);

            System.out.println("Expected signature: " + expectedSignature.substring(0, Math.min(20, expectedSignature.length())) + "...");
            System.out.println("Received signature: " + signature.substring(0, Math.min(20, signature.length())) + "...");
            System.out.println("Signatures match: " + signature.equals(expectedSignature));

            if (!signature.equals(expectedSignature)) {
                System.out.println("❌ SIGNATURE MISMATCH!");
                System.out.println("   This usually means:");
                System.out.println("   1. The token was generated with a different secret key");
                System.out.println("   2. The token was modified after generation");
                System.out.println("   3. Encoding issues (URL-safe vs standard Base64)");
                return false;
            }
            System.out.println("✓ Signature valid");

            // ✅ Parsear payload
            JsonNode payloadNode = objectMapper.readTree(decodedPayload);

            // ✅ Verificar expiración
            JsonNode expNode = payloadNode.get("exp");
            if (expNode == null) {
                System.out.println("❌ No 'exp' claim in token");
                return false;
            }

            long exp = expNode.asLong();
            long now = Instant.now().getEpochSecond();

            System.out.println("Token exp: " + exp + " (" + Instant.ofEpochSecond(exp) + ")");
            System.out.println("Current time: " + now + " (" + Instant.ofEpochSecond(now) + ")");
            System.out.println("Time until expiry: " + (exp - now) + " seconds");

            if (now >= exp) {
                System.out.println("❌ TOKEN EXPIRED! (expired " + (now - exp) + " seconds ago)");
                return false;
            }
            System.out.println("✓ Token not expired");

            // ✅ Verificar issuer
            JsonNode issNode = payloadNode.get("iss");
            String issuer = issNode != null ? issNode.asText() : null;
            System.out.println("Issuer: " + issuer);

            if (!"https://unmsm.edu.pe".equals(issuer)) {
                System.out.println("❌ Invalid issuer (expected 'https://unmsm.edu.pe')");
                return false;
            }
            System.out.println("✓ Issuer valid");

            // ✅ Verificar audience
            JsonNode audNode = payloadNode.get("aud");
            String audience = audNode != null ? audNode.asText() : null;
            System.out.println("Audience: " + audience);

            if (!"upeu-sis".equals(audience)) {
                System.out.println("❌ Invalid audience (expected 'upeu-sis')");
                return false;
            }
            System.out.println("✓ Audience valid");

            // ✅ Mostrar info del usuario
            String sub = payloadNode.has("sub") ? payloadNode.get("sub").asText() : "N/A";
            String userId = payloadNode.has("userId") ? payloadNode.get("userId").asText() : "N/A";
            String roles = payloadNode.has("roles") ? payloadNode.get("roles").toString() : "[]";

            System.out.println("Subject (username): " + sub);
            System.out.println("User ID: " + userId);
            System.out.println("Roles: " + roles);

            System.out.println("✅ TOKEN VALID!");
            System.out.println("=== JWT VALIDATOR END ===\n");
            return true;

        } catch (Exception e) {
            System.out.println("❌ Exception during validation: " + e.getClass().getSimpleName());
            System.out.println("   Message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=== JWT VALIDATOR END (ERROR) ===\n");
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String[] parts = cleanToken.split("\\.");
            String payload = parts[1];
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            return payloadNode.has("sub") ? payloadNode.get("sub").asText() : null;
        } catch (Exception e) {
            System.out.println("Error getting username from token: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ CORREGIDO: Retorna String (UUID) en lugar de Long
     */
    public String getUserIdFromToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String[] parts = cleanToken.split("\\.");
            String payload = parts[1];
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            return payloadNode.has("userId") ? payloadNode.get("userId").asText() : null;
        } catch (Exception e) {
            System.out.println("Error getting userId from token: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ NUEVO: Retorna UUID directamente
     */
    public UUID getUserIdAsUUID(String token) {
        try {
            String userId = getUserIdFromToken(token);
            return userId != null ? UUID.fromString(userId) : null;
        } catch (IllegalArgumentException e) {
            System.out.println("Error parsing userId as UUID: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Obtener los roles del usuario (claim "roles", array dinámico de user_roles)
     */
    public List<String> getRolesFromToken(String token) {
        List<String> roles = new ArrayList<>();
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String[] parts = cleanToken.split("\\.");
            String payload = parts[1];
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            JsonNode rolesNode = payloadNode.get("roles");
            if (rolesNode != null && rolesNode.isArray()) {
                rolesNode.forEach(node -> roles.add(node.asText()));
            }
        } catch (Exception e) {
            System.out.println("Error getting roles from token: " + e.getMessage());
        }
        return roles;
    }

    /**
     * ✅ NUEVO: Obtener email del usuario
     */
    public String getEmailFromToken(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String[] parts = cleanToken.split("\\.");
            String payload = parts[1];
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            return payloadNode.has("email") ? payloadNode.get("email").asText() : null;
        } catch (Exception e) {
            System.out.println("Error getting email from token: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ NUEVO: Verificar si el token está expirado (sin validar firma)
     */
    public boolean isTokenExpired(String token) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String[] parts = cleanToken.split("\\.");
            String payload = parts[1];
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(decodedPayload);

            long exp = payloadNode.get("exp").asLong();
            long now = Instant.now().getEpochSecond();

            return now >= exp;
        } catch (Exception e) {
            return true; // Si hay error, asumir expirado
        }
    }

    /**
     * ✅ NUEVO: Obtener cualquier claim como String
     */
    public String getClaimAsString(String token, String claimName) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String[] parts = cleanToken.split("\\.");
            String payload = parts[1];
            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(decodedPayload);
            return payloadNode.has(claimName) ? payloadNode.get(claimName).asText() : null;
        } catch (Exception e) {
            System.out.println("Error getting claim '" + claimName + "' from token: " + e.getMessage());
            return null;
        }
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}