package unmsm.edu.pe.security.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import unmsm.edu.pe.security.application.dto.RoleDTO;
import unmsm.edu.pe.security.domain.services.UserRoleService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/user-roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Roles", description = "API de gestión de roles por usuario")
public class UserRoleResource {

    private static final Logger LOG = Logger.getLogger(UserRoleResource.class);

    @Inject
    UserRoleService userRoleService;

    @GET
    @Path("/user/{userId}")
    @Operation(
            summary = "Listar roles del usuario",
            description = "Obtiene todos los roles disponibles marcando cuáles están asignados al usuario"
    )
    public Response findAllRolesSelectedByUserId(
            @PathParam("userId")
            @Parameter(description = "ID del usuario", required = true) UUID userId) {

        try {
            List<RoleDTO.Response> roles = userRoleService.findAllRolesSelectedByUserId(userId);
            LOG.info("Roles obtenidos para usuario " + userId + ": " + roles.size());
            return Response.ok(roles).build();
        } catch (NotFoundException e) {
            LOG.error("Usuario no encontrado: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error al obtener roles del usuario: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Operation(
            summary = "Asignar roles a usuario",
            description = "Asigna (reemplaza) el conjunto de roles de un usuario"
    )
    public Response save(
            @Parameter(description = "Datos de asignación de roles", required = true)
            RoleDTO.Request request) {

        try {
            if (request.getUserId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "userId is required"))
                        .build();
            }

            if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "roleIds cannot be empty"))
                        .build();
            }

            LOG.info("Asignando " + request.getRoleIds().size() +
                    " roles al usuario: " + request.getUserId());

            List<RoleDTO.Response> roles = userRoleService.save(request);

            return Response.status(Response.Status.CREATED)
                    .entity(roles)
                    .build();

        } catch (NotFoundException e) {
            LOG.error("Usuario no encontrado: " + e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error al asignar roles: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}