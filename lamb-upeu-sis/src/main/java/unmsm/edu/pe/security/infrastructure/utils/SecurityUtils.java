package unmsm.edu.pe.security.infrastructure.utils;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Utilidad para extraer información del usuario autenticado desde el JWT
 * ✅ Usa JwtTokenValidator (nuestro sistema personalizado)
 * ✅ NO depende de SmallRye JWT / SecurityIdentity
 */
@RequestScoped
public class SecurityUtils {

    private static final Logger LOG = Logger.getLogger(SecurityUtils.class);

    @Inject
    JwtTokenValidator jwtTokenValidator;

    @Context
    HttpHeaders httpHeaders;

    /**
     * ✅ Obtener el token del header Authorization
     */
    public String getAccessToken() {
        try {
            if (httpHeaders == null) {
                LOG.debug("HttpHeaders is null");
                return null;
            }

            String authHeader = httpHeaders.getHeaderString("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                LOG.debug("No Authorization header present");
                return null;
            }

            if (!authHeader.startsWith("Bearer ")) {
                LOG.debug("Authorization header doesn't start with 'Bearer '");
                return null;
            }

            return authHeader.substring(7).trim();
        } catch (Exception e) {
            LOG.warn("Error getting access token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Alias para getAccessToken
     */
    public String getRawToken() {
        return getAccessToken();
    }

    /**
     * ✅ Verificar si hay un token válido
     */
    private boolean hasValidToken() {
        String token = getAccessToken();
        return token != null && jwtTokenValidator.validateToken(token);
    }

    /**
     * ✅ Verificar si el usuario está autenticado
     */
    public boolean isAuthenticated() {
        return hasValidToken();
    }

    /**
     * ✅ Obtener el ID del usuario (claim "userId")
     */
    public String getCurrentUserId() {
        try {
            String token = getAccessToken();
            if (token == null) {
                return null;
            }
            return jwtTokenValidator.getUserIdFromToken(token);
        } catch (Exception e) {
            LOG.warn("Error getting current user ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Obtener el ID del usuario como UUID
     */
    public UUID getCurrentUserIdAsUUID() {
        try {
            String userId = getCurrentUserId();
            if (userId == null || userId.isBlank()) {
                return null;
            }
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            LOG.warn("User ID is not a valid UUID: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Obtener el username (claim "sub")
     */
    public String getCurrentUserName() {
        try {
            String token = getAccessToken();
            if (token == null) {
                return null;
            }
            return jwtTokenValidator.getUsernameFromToken(token);
        } catch (Exception e) {
            LOG.warn("Error getting username: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Obtener el email
     */
    public String getCurrentUserEmail() {
        try {
            String token = getAccessToken();
            if (token == null) {
                return null;
            }
            return jwtTokenValidator.getEmailFromToken(token);
        } catch (Exception e) {
            LOG.warn("Error getting email: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Obtener roles como array (claim "roles", dinámico de user_roles)
     */
    public String[] getCurrentUserRoles() {
        try {
            String token = getAccessToken();
            if (token == null) {
                return new String[0];
            }
            return jwtTokenValidator.getRolesFromToken(token).toArray(new String[0]);
        } catch (Exception e) {
            LOG.warn("Error getting roles: " + e.getMessage());
            return new String[0];
        }
    }

    /**
     * ✅ Obtener el primer rol del usuario (compatibilidad)
     */
    public String getCurrentUserRole() {
        String[] roles = getCurrentUserRoles();
        return roles.length > 0 ? roles[0] : null;
    }

    /**
     * ✅ Verificar si tiene un rol específico
     */
    public boolean hasRole(String role) {
        if (role == null) return false;
        for (String current : getCurrentUserRoles()) {
            if (role.equalsIgnoreCase(current)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ✅ Verificar si es admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN") || hasRole("ADMINISTRADOR") || hasRole("ROLE_ADMIN");
    }

    /**
     * ✅ Exigir que el usuario tenga al menos uno de los roles (códigos) dados.
     * Lanza 403 (ForbiddenException) si no cumple.
     */
    public void requireAnyRole(String... roleCodes) {
        for (String code : roleCodes) {
            if (hasRole(code)) {
                return;
            }
        }
        throw new ForbiddenException("Requiere uno de los roles: " + String.join(", ", roleCodes));
    }

    /**
     * ✅ Obtener nombre completo
     */
    public String getCurrentUserFullName() {
        try {
            String token = getAccessToken();
            if (token == null) {
                return null;
            }

            String firstName = jwtTokenValidator.getClaimAsString(token, "firstName");
            String lastName = jwtTokenValidator.getClaimAsString(token, "lastName");

            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            } else if (firstName != null) {
                return firstName;
            } else if (lastName != null) {
                return lastName;
            } else {
                return getCurrentUserName();
            }
        } catch (Exception e) {
            LOG.warn("Error getting full name: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Obtener un claim personalizado
     */
    @SuppressWarnings("unchecked")
    public <T> T getClaim(String claimName) {
        try {
            String token = getAccessToken();
            if (token == null) {
                return null;
            }
            return (T) jwtTokenValidator.getClaimAsString(token, claimName);
        } catch (Exception e) {
            LOG.warn("Error getting claim '" + claimName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Obtener el nombre del principal (username)
     */
    public String getPrincipalName() {
        return getCurrentUserName();
    }

    /**
     * ✅ Obtener información completa del usuario
     */
    public UserInfo getCurrentUserInfo() {
        return UserInfo.builder()
                .userId(getCurrentUserId())
                .username(getCurrentUserName())
                .fullName(getCurrentUserFullName())
                .email(getCurrentUserEmail())
                .role(getCurrentUserRole())
                .roles(getCurrentUserRoles())
                .isAuthenticated(isAuthenticated())
                .isAdmin(isAdmin())
                .build();
    }

    /**
     * Clase para encapsular información del usuario
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserInfo {
        private String userId;
        private String username;
        private String fullName;
        private String email;
        private String role;
        private String[] roles;
        private Boolean isAuthenticated;
        private Boolean isAdmin;
    }
}