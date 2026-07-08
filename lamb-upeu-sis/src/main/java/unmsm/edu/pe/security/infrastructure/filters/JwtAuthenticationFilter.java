package unmsm.edu.pe.security.infrastructure.filters;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import unmsm.edu.pe.security.infrastructure.utils.JwtTokenValidator;
import unmsm.edu.pe.shared.context.AuditContext;

import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    static {
        System.out.println("=== JwtAuthenticationFilter CLASS LOADED ===");
    }

    @Inject
    JwtTokenValidator jwtTokenValidator;

    @Inject
    AuditContext auditContext;

    public JwtAuthenticationFilter() {
        System.out.println("=== JwtAuthenticationFilter CONSTRUCTOR called ===");
    }

    @PostConstruct
    void init() {
        System.out.println("=== JwtAuthenticationFilter @PostConstruct called ===");
        System.out.println("JwtTokenValidator injected: " + (jwtTokenValidator != null));
        System.out.println("AuditContext injected: " + (auditContext != null));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        String fullUri = requestContext.getUriInfo().getRequestUri().toString();

        System.out.println("\n=== JWT FILTER EXECUTED ===");
        System.out.println("Full URI: " + fullUri);
        System.out.println("Path: " + path);
        System.out.println("Method: " + method);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            System.out.println("DECISION: Skipping authentication for public endpoint");
            System.out.println("=== JWT FILTER END (PUBLIC) ===\n");
            return;
        }

        System.out.println("DECISION: Authentication required for this endpoint");

        // Get Authorization header
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        System.out.println("Raw Authorization Header: '" + authHeader + "'");

        if (authHeader != null) {
            System.out.println("Header length: " + authHeader.length());
            System.out.println("Header starts with 'Bearer ': " + authHeader.startsWith("Bearer "));
        }

        if (authHeader == null || authHeader.trim().isEmpty()) {
            System.out.println("ERROR: Missing authorization header");
            abortWithUnauthorized(requestContext, "Missing or invalid authorization header");
            return;
        }

        if (!authHeader.startsWith("Bearer ")) {
            System.out.println("ERROR: Authorization header must start with 'Bearer '");
            abortWithUnauthorized(requestContext, "Authorization header must start with 'Bearer '");
            return;
        }

        String token = authHeader.substring(7).trim();
        System.out.println("Token extracted with Bearer prefix");

        if (token.trim().isEmpty()) {
            System.out.println("ERROR: Empty token after extraction");
            abortWithUnauthorized(requestContext, "Empty authorization token");
            return;
        }

        System.out.println("Token extracted (length: " + token.length() + ")");
        System.out.println("Token preview: " + token.substring(0, Math.min(30, token.length())) + "...");

        // Validate token
        try {
            boolean isValid = jwtTokenValidator.validateToken(token);
            System.out.println("Token validation result: " + isValid);

            if (!isValid) {
                System.out.println("ERROR: Token validation failed");
                abortWithUnauthorized(requestContext, "Invalid or expired token");
                return;
            }

            // Set current user for auditing
            String username = jwtTokenValidator.getUsernameFromToken(token);
            if (username != null) {
                auditContext.setCurrentUser(username);
                System.out.println("SUCCESS: User authenticated: " + username);
            } else {
                System.out.println("WARNING: Could not extract username from token");
            }

            System.out.println("SUCCESS: Authentication completed successfully");
            System.out.println("=== JWT FILTER END (SUCCESS) ===\n");

        } catch (Exception e) {
            System.out.println("ERROR: Exception during token validation: " + e.getMessage());
            e.printStackTrace();
            abortWithUnauthorized(requestContext, "Authentication failed");
        }
    }

    private boolean isPublicEndpoint(String path) {
        // Normalize path
        String normalizedPath = path.replaceAll("^/+|/+$", "").toLowerCase();

        System.out.println("Checking if path is public: '" + normalizedPath + "'");

        boolean isPublic = normalizedPath.isEmpty() ||
                normalizedPath.equals("openapi") ||
                normalizedPath.equals("swagger-ui") ||
                normalizedPath.startsWith("swagger-ui/") ||
                normalizedPath.startsWith("q/") ||
                normalizedPath.startsWith("health") ||
                normalizedPath.startsWith("metrics") ||
                normalizedPath.startsWith("api/v1/auth/") ||
                normalizedPath.contains("/auth/") ||
                normalizedPath.endsWith("/auth") ||
                // Additional patterns for auth endpoints
                (normalizedPath.contains("auth") &&
                        (normalizedPath.contains("login") ||
                                normalizedPath.contains("register") ||
                                normalizedPath.contains("refresh")))
                ;

        System.out.println("Path '" + normalizedPath + "' is public: " + isPublic);
        return isPublic;
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        System.out.println("ABORT: Returning 401 Unauthorized - " + message);
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"" + message + "\"}")
                        .type("application/json")
                        .build()
        );
        System.out.println("=== JWT FILTER END (UNAUTHORIZED) ===\n");
    }
}