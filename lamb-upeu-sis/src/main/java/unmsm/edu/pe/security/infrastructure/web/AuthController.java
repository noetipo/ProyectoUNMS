// src/main/java/upeu/edu/pe/security/infrastructure/web/AuthController.java
package unmsm.edu.pe.security.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.application.dto.*;
import unmsm.edu.pe.security.domain.services.AuthService;
import unmsm.edu.pe.shared.response.ApiResponse;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "User authentication operations with MicroProfile JWT")
public class AuthController {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    @APIResponse(responseCode = "200", description = "Login successful")
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    @APIResponse(responseCode = "400", description = "Invalid request data")
    public Response login(@Valid LoginRequestDto loginRequest) {
        try {
            AuthResponseDto authResponse = authService.login(loginRequest);
            return Response.ok(ApiResponse.success("Login successful", authResponse)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Login failed", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/register")
    @Operation(summary = "User registration", description = "Create new user account and return JWT tokens")
    @APIResponse(responseCode = "201", description = "User registered successfully")
    @APIResponse(responseCode = "409", description = "Username or email already exists")
    @APIResponse(responseCode = "400", description = "Invalid request data")
    public Response register(@Valid RegisterRequestDto registerRequest) {
        System.out.println("=====> "+registerRequest.toString());
        try {
            AuthResponseDto authResponse = authService.register(registerRequest);
            return Response.status(Response.Status.CREATED)
                    .entity(ApiResponse.success("User registered successfully", authResponse))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ApiResponse.error("Registration failed", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    @APIResponse(responseCode = "200", description = "Token refreshed successfully")
    @APIResponse(responseCode = "401", description = "Invalid or expired refresh token")
    public Response refreshToken(@Valid RefreshTokenRequestDto refreshRequest) {
        try {
            TokenResponseDto tokenResponse = authService.refreshToken(refreshRequest);
            return Response.ok(ApiResponse.success("Token refreshed successfully", tokenResponse)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Token refresh failed", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/logout")
    @Operation(summary = "User logout", description = "Revoke refresh token and logout user")
    @APIResponse(responseCode = "200", description = "Logout successful")
    @APIResponse(responseCode = "401", description = "Invalid refresh token")
    public Response logout(@Valid RefreshTokenRequestDto logoutRequest) {
        try {
            authService.logout(logoutRequest.getRefreshToken());
            return Response.ok(ApiResponse.success("Logout successful")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Logout failed", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/logout-all")
    @Operation(summary = "Logout all devices", description = "Revoke all refresh tokens for user")
    @APIResponse(responseCode = "200", description = "Logout from all devices successful")
    @APIResponse(responseCode = "404", description = "User not found")
    @APIResponse(responseCode = "400", description = "Username header is required")
    public Response logoutAllDevices(
            @Parameter(description = "Username from JWT token")
            @HeaderParam("X-Username") String username) {

        if (username == null || username.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Username header is required"))
                    .build();
        }

        try {
            authService.logoutAllDevices(username);
            return Response.ok(ApiResponse.success("Logout from all devices successful")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiResponse.error("Logout all devices failed", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/validate")
    @Operation(summary = "Validate token", description = "Validate if the provided token is still valid")
    @APIResponse(responseCode = "200", description = "Token is valid")
    @APIResponse(responseCode = "401", description = "Token is invalid or expired")
    public Response validateToken(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Missing or invalid authorization header"))
                    .build();
        }

        String token = authHeader.substring(7);

        // You can add token validation logic here using JwtTokenValidator
        // For now, just return success if header format is correct
        return Response.ok(ApiResponse.success("Token format is valid")).build();
    }

    @GET
    @Path("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information from JWT token")
    @APIResponse(responseCode = "200", description = "User information retrieved")
    @APIResponse(responseCode = "401", description = "Invalid or missing token")
    public Response getCurrentUser(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.error("Missing or invalid authorization header"))
                    .build();
        }

        // Extract token and get user info
        // This would typically parse the JWT and extract user claims
        String token = authHeader.substring(7);

        // For now, return a placeholder response
        // In a real implementation, you'd parse the JWT and extract user info
        return Response.ok(ApiResponse.success("User information from JWT token",
                "Token received: " + token.substring(0, Math.min(20, token.length())) + "...")).build();
    }
}