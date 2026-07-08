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
import unmsm.edu.pe.security.application.dto.RoleModuleDTO;
import unmsm.edu.pe.security.application.dto.RoleRequestDto;
import unmsm.edu.pe.security.application.dto.RoleResponseDto;
import unmsm.edu.pe.security.domain.services.RoleService;

import java.util.Map;
import java.util.UUID;

@Path("/api/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "API de gestión de roles del sistema")
public class RoleController {

    private static final Logger LOG = Logger.getLogger(RoleController.class);

    @Inject
    RoleService roleService;

    @GET
    @Operation(summary = "Listar roles", description = "Obtiene lista paginada de roles con búsqueda opcional")
    public Response list(
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Número de página") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Tamaño de página") Integer size,
            @QueryParam("name")
            @Parameter(description = "Filtro por nombre (opcional)") String name) {

        try {
            Map<String, Object> response = roleService.list(page, size, name);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error al listar roles: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Operation(summary = "Crear rol", description = "Crea un nuevo rol en el sistema")
    public Response save(
            @Parameter(description = "Datos del rol a crear", required = true)
            RoleRequestDto roleRequestDto) {
        try {
            RoleResponseDto savedRole = roleService.save(roleRequestDto);
            return Response.status(Response.Status.CREATED)
                    .entity(savedRole)
                    .build();
        } catch (Exception e) {
            LOG.error("Error al guardar rol: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/module")
    @Operation(summary = "Asignar módulos a rol",
            description = "Asigna o desasigna módulos a un rol específico")
    public Response saveModuleAssignment(
            @Parameter(description = "Datos de asignación de módulos al rol", required = true)
            RoleModuleDTO roleModuleDTO) {
        try {
            RoleResponseDto role = roleService.saveModuleAssignment(roleModuleDTO);
            return Response.ok(role).build();
        } catch (Exception e) {
            LOG.error("Error al asignar módulos al rol: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener rol por ID", description = "Obtiene los detalles de un rol específico")
    public Response findById(
            @PathParam("id")
            @Parameter(description = "ID del rol", required = true) UUID id) {
        try {
            RoleResponseDto role = roleService.findById(id);
            return Response.ok(role).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error al buscar rol: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar rol", description = "Actualiza los datos de un rol existente")
    public Response update(
            @PathParam("id")
            @Parameter(description = "ID del rol", required = true) UUID id,
            @Parameter(description = "Datos actualizados del rol", required = true)
            RoleRequestDto roleRequestDto) {
        try {
            RoleResponseDto updatedRole = roleService.update(id, roleRequestDto);
            return Response.ok(updatedRole).build();
        } catch (Exception e) {
            LOG.error("Error al actualizar rol: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar rol", description = "Elimina un rol del sistema (soft delete)")
    public Response delete(
            @PathParam("id")
            @Parameter(description = "ID del rol", required = true) UUID id) {
        try {
            roleService.delete(id);
            Map<String, Object> response = roleService.list(0, 20, null);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error al eliminar rol: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}