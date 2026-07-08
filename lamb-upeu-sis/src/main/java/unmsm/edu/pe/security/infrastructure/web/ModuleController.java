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
import unmsm.edu.pe.security.application.dto.MenuDto;
import unmsm.edu.pe.security.application.dto.ModuleDto;
import unmsm.edu.pe.security.application.dto.ModuleSelectedDto;
import unmsm.edu.pe.security.application.dto.PaginatedResponseDto;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.services.ModuleService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/modules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Modules", description = "API de gestión de módulos del sistema")
public class ModuleController {

    private static final Logger LOG = Logger.getLogger(ModuleController.class);

    @Inject
    ModuleService moduleService;

    @GET
    @Operation(summary = "Listar módulos padre", description = "Obtiene lista paginada de módulos padre con búsqueda opcional")
    public Response getParentModules(
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Número de página") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Tamaño de página") Integer size,
            @QueryParam("name")
            @Parameter(description = "Filtro por nombre (opcional)") String name) {

        try {
            PaginatedResponseDto<Module> response = moduleService.list(page, size, name);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Error al listar módulos: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/menu")
    @Operation(summary = "Obtener menú", description = "Obtiene el menú de módulos del usuario actual")
    public Response getMenu() {
        try {
            List<MenuDto> menu = moduleService.listMenu();
            LOG.info("Menú obtenido exitosamente. Items: " + menu.size());
            return Response.ok(menu).build();
        } catch (Exception e) {
            LOG.error("Error al obtener menú: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/menu/role/{roleId}")
    @Operation(summary = "Obtener menú por rol", description = "Obtiene el árbol de módulos asignados a un rol específico")
    public Response getMenuByRole(
            @PathParam("roleId")
            @Parameter(description = "ID del rol", required = true) UUID roleId) {
        try {
            List<MenuDto> menu = moduleService.listMenuByRole(roleId);
            return Response.ok(menu).build();
        } catch (Exception e) {
            LOG.error("Error al obtener menú por rol: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Operation(summary = "Crear módulo", description = "Crea un nuevo módulo en el sistema")
    public Response save(
            @Parameter(description = "Datos del módulo a crear", required = true) ModuleDto moduleDto) {
        try {
            Module module = moduleService.save(moduleDto);
            return Response.status(Response.Status.CREATED)
                    .entity(module)
                    .build();
        } catch (Exception e) {
            LOG.error("Error al guardar módulo: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener módulo por ID", description = "Obtiene los detalles de un módulo específico")
    public Response findById(
            @PathParam("id")
            @Parameter(description = "ID del módulo", required = true) UUID id) {
        try {
            return moduleService.findById(id)
                    .map(module -> Response.ok(module).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Módulo no encontrado"))
                            .build());
        } catch (Exception e) {
            LOG.error("Error al buscar módulo: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/modules-selected/roleId/{roleId}/parentModuleId/{parentModuleId}")
    @Operation(summary = "Obtener módulos asignados a rol",
            description = "Obtiene los módulos asignados a un rol específico bajo un módulo padre")
    public Response findAllModulesSelected(
            @PathParam("roleId")
            @Parameter(description = "ID del rol", required = true) UUID roleId,
            @PathParam("parentModuleId")
            @Parameter(description = "ID del módulo padre", required = true) UUID parentModuleId) {
        try {
            List<ModuleSelectedDto> modules = moduleService.findAllModulesAsignetToRol(roleId, parentModuleId);
            return Response.ok(modules).build();
        } catch (Exception e) {
            LOG.error("Error al obtener módulos seleccionados: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar módulo", description = "Actualiza los datos de un módulo existente")
    public Response update(
            @PathParam("id")
            @Parameter(description = "ID del módulo", required = true) UUID id,
            @Parameter(description = "Datos actualizados del módulo", required = true) ModuleDto moduleDto) {
        try {
            Module module = moduleService.update(id, moduleDto);
            return Response.ok(module).build();
        } catch (Exception e) {
            LOG.error("Error al actualizar módulo: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar módulo", description = "Elimina un módulo del sistema")
    public Response delete(
            @PathParam("id")
            @Parameter(description = "ID del módulo", required = true) UUID id) {
        try {
            moduleService.delete(id);
            // Respuesta simple confirmando la eliminación
            return Response.ok(Map.of(
                    "message", "Módulo eliminado exitosamente",
                    "id", id.toString()
            )).build();
        } catch (Exception e) {
            LOG.error("Error al eliminar módulo: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}