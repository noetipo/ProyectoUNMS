package unmsm.edu.pe.security.infrastructure.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import unmsm.edu.pe.security.infrastructure.filters.JwtAuthenticationFilter;

import java.util.Set;

@ApplicationPath("/")
@OpenAPIDefinition(
        info = @Info(
                title = "Lamb UPEU SIS API",
                version = "1.0.0",
                description = "Modular Monolith API for Lamb UPEU System with JWT Authentication",
                contact = @Contact(
                        name = "Development Team",
                        email = "dev@unmsm.edu.pe"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Development server")
        }
)
@SecurityScheme(
        securitySchemeName = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token for authentication. Obtain it from /api/v1/auth/register or /api/v1/auth/login"
)
public class OpenApiConfig extends Application {

    public OpenApiConfig() {
        System.out.println("OpenApiConfig initialized!");
    }

    /*@Override
    public Set<Class<?>> getClasses() {
        System.out.println("Registering JAX-RS components including JWT filter...");
        return Set.of(JwtAuthenticationFilter.class);
    }*/
}