package unmsm.edu.pe.security.infrastructure.utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@ApplicationScoped
public class JwtTokenGenerator {

    @ConfigProperty(name = "jwt.duration", defaultValue = "3600")
    Long jwtDuration;

    @ConfigProperty(name = "jwt.refresh.duration", defaultValue = "604800")
    Long refreshDuration;

    @Inject
    UserRoleAssignmentRepository userRoleRepository;

    private static final String SECRET_KEY = "mySecretKey1234567890abcdefghij"; // 32 caracteres

    public String generateAccessToken(User user) {
        try {
            long expirationTime = Instant.now().plusSeconds(jwtDuration).getEpochSecond();

            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payload = String.format(
                    "{\"iss\":\"https://unmsm.edu.pe\"," +
                            "\"aud\":\"upeu-sis\"," +
                            "\"sub\":\"%s\"," +
                            "\"userId\":\"%s\"," +  // ✅ Cambiar %d a "%s"
                            "\"email\":\"%s\"," +
                            "\"firstName\":\"%s\"," +
                            "\"lastName\":\"%s\"," +
                            "\"roles\":%s," +
                            "\"exp\":%d," +
                            "\"iat\":%d}",
                    user.getUsername(),
                    user.getId(),  // ✅ Ahora se formatea como String
                    user.getEmail(),
                    user.getFirstName() != null ? user.getFirstName() : "",
                    user.getLastName() != null ? user.getLastName() : "",
                    rolesAsJsonArray(user),
                    expirationTime,
                    Instant.now().getEpochSecond()
            );

            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(header.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

            String data = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256(data, SECRET_KEY);

            return data + "." + signature;
        } catch (Exception e) {
            System.out.println("Error generando access token: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error generando access token", e);
        }
    }

    public String generateRefreshToken(User user) {
        try {
            long expirationTime = Instant.now().plusSeconds(refreshDuration).getEpochSecond();
            long issuedAt = Instant.now().getEpochSecond();

            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payload = String.format(
                    "{\"iss\":\"https://unmsm.edu.pe\"," +
                            "\"aud\":\"upeu-sis\"," +
                            "\"sub\":\"%s\"," +
                            "\"userId\":\"%s\"," +  // ✅ Cambiar %d a "%s"
                            "\"type\":\"refresh\"," +
                            "\"exp\":%d," +
                            "\"iat\":%d}",
                    user.getUsername(),
                    user.getId().toString(),  // ✅ Convertir UUID a String
                    expirationTime,
                    issuedAt
            );

            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(header.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

            String data = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256(data, SECRET_KEY);

            return data + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Error generando refresh token", e);
        }
    }

    /**
     * Construye un array JSON con los CÓDIGOS de los roles activos del usuario
     * (identificadores estables para autorización), leídos de user_roles.
     */
    private String rolesAsJsonArray(User user) {
        List<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < roles.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJson(roles.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    public Long getDuration() {
        return jwtDuration;
    }

    public Long getRefreshDuration() {
        return refreshDuration;
    }
}