package unmsm.edu.pe.personas.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.personas.domain.services.EstudianteReportService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.UUID;

@Path("/api/estudiantes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Estudiantes", description = "Reporte de estudiantes")
public class EstudianteResource {

    @Inject EstudianteReportService estudianteReportService;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Listar estudiantes", description = "Listado paginado con filtros")
    public Response listar(
            @QueryParam("search") String search,
            @QueryParam("facultadId") String facultadId,
            @QueryParam("programaId") String programaId,
            @QueryParam("condicion") String condicion,
            @QueryParam("nivel") String nivel,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        UUID fac = (facultadId != null && !facultadId.isBlank()) ? UUID.fromString(facultadId) : null;
        UUID prog = (programaId != null && !programaId.isBlank()) ? UUID.fromString(programaId) : null;
        var pageResponse = estudianteReportService.listar(search, fac, prog, condicion, nivel, page, size);
        return Response.ok(ApiResponse.success("Estudiantes recuperados", pageResponse)).build();
    }

    @GET
    @Path("/resumen")
    @Operation(summary = "Resumen de estudiantes", description = "Conteos para las tarjetas")
    public Response resumen() {
        guard();
        return Response.ok(ApiResponse.success("Resumen", estudianteReportService.resumen())).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("ADMIN", "SECRETARIA", "COORDINADOR");
    }
}
