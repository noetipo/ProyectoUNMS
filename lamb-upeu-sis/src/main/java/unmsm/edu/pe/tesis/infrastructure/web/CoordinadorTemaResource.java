package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.application.dto.RegistrarTemaRequest;
import unmsm.edu.pe.tesis.domain.services.CoordinadorTemaService;

import java.util.UUID;

/**
 * Inicio del proceso de tesis a cargo del coordinador: reporte de estudiantes por
 * estado de tema y registro/edición del tema. El coordinador ve TODOS los programas
 * (con filtro por programa); no hay restricción coordinador↔programa en el modelo.
 */
@Path("/api/coordinador")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Coordinador · Registro de tema", description = "Reporte de tema y registro de tesis (coordinador)")
public class CoordinadorTemaResource {

    @Inject CoordinadorTemaService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Path("/estudiantes-tema/resumen")
    @Operation(summary = "Resumen de estudiantes por estado de tema (total, con tema, sin tema)")
    public Response resumen(@QueryParam("facultadId") UUID facultadId, @QueryParam("programaId") UUID programaId) {
        guardRead();
        return Response.ok(ApiResponse.success("Resumen", service.resumen(facultadId, programaId))).build();
    }

    @GET
    @Path("/estudiantes-tema")
    @Operation(summary = "Reporte de estudiantes con/sin tema (búsqueda, filtros, paginación)")
    public Response estudiantesTema(
            @QueryParam("facultadId") UUID facultadId,
            @QueryParam("programaId") UUID programaId,
            @QueryParam("conTema") Boolean conTema,
            @QueryParam("buscar") String buscar,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guardRead();
        return Response.ok(ApiResponse.success("Estudiantes recuperados",
                service.listar(facultadId, programaId, conTema, buscar, page, size))).build();
    }

    @POST
    @Path("/estudiantes/{estudianteId}/tema")
    @Operation(summary = "Registrar el tema de un estudiante (crea la tesis)")
    public Response registrar(@PathParam("estudianteId") UUID estudianteId,
                              @Valid RegistrarTemaRequest request) {
        guardWrite();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Tema registrado", service.registrarTema(estudianteId, request)))
                .build();
    }

    @PUT
    @Path("/estudiantes/{estudianteId}/tema")
    @Operation(summary = "Editar el tema registrado de un estudiante")
    public Response editar(@PathParam("estudianteId") UUID estudianteId,
                           @Valid RegistrarTemaRequest request) {
        guardWrite();
        return Response.ok(ApiResponse.success("Tema actualizado", service.editarTema(estudianteId, request))).build();
    }

    // ── guards ──
    /** Lectura del reporte: coordinadores + gestión (admin/secretaría), solo lectura. */
    private void guardRead() {
        securityUtils.requireAnyRole("COORDINADOR", "COORD_PROG", "COORD_SEC", "ADMIN", "SECRETARIA");
    }

    /** Registro/edición del tema: solo coordinadores con edición. */
    private void guardWrite() {
        securityUtils.requireAnyRole("COORDINADOR", "COORD_PROG");
    }
}
