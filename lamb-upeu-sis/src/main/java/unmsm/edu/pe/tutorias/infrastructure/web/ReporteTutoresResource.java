package unmsm.edu.pe.tutorias.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tutorias.domain.services.ReporteTutoresService;

import java.util.UUID;

@Path("/api/reportes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reporte de tutores", description = "Tutores con sus estudiantes (vigente), con exportación")
public class ReporteTutoresResource {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject ReporteTutoresService reporteService;
    @Inject SecurityUtils securityUtils;

    @GET
    @Path("/tutores/resumen")
    @Operation(summary = "Resumen del reporte (respeta filtro de programa)")
    public Response resumen(@QueryParam("facultadId") String facultadId, @QueryParam("programaId") String programaId) {
        guard();
        return Response.ok(ApiResponse.success("Resumen", reporteService.resumen(uuid(facultadId), uuid(programaId)))).build();
    }

    @GET
    @Path("/tutores")
    @Operation(summary = "Lista de tutores paginada (sin estudiantes)")
    public Response tutores(
            @QueryParam("buscar") String buscar,
            @QueryParam("facultadId") String facultadId,
            @QueryParam("programaId") String programaId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Tutores recuperados",
                reporteService.listar(buscar, uuid(facultadId), uuid(programaId), page, size))).build();
    }

    @GET
    @Path("/tutores/{id}/estudiantes")
    @Operation(summary = "Estudiantes vigentes de un tutor (lazy)")
    public Response estudiantesDeTutor(
            @PathParam("id") UUID id,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {
        guard();
        return Response.ok(ApiResponse.success("Estudiantes del tutor",
                reporteService.estudiantesDeTutor(id, page, size))).build();
    }

    @GET
    @Path("/estudiantes-sin-tutor")
    @Operation(summary = "Estudiantes sin tutoría vigente")
    public Response sinTutor(
            @QueryParam("facultadId") String facultadId,
            @QueryParam("programaId") String programaId,
            @QueryParam("buscar") String buscar,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Estudiantes sin tutor",
                reporteService.estudiantesSinTutor(uuid(facultadId), uuid(programaId), buscar, page, size))).build();
    }

    @GET
    @Path("/tutores/export")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Exportar el reporte (formato=pdf|xlsx) con todos los datos del filtro")
    public Response export(
            @QueryParam("formato") @DefaultValue("xlsx") String formato,
            @QueryParam("buscar") String buscar,
            @QueryParam("facultadId") String facultadId,
            @QueryParam("programaId") String programaId) {
        guard();
        UUID fac = uuid(facultadId);
        UUID prog = uuid(programaId);
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        byte[] bytes = pdf ? reporteService.exportarPdf(buscar, fac, prog) : reporteService.exportarExcel(buscar, fac, prog);
        String filename = "reporte-tutores." + (pdf ? "pdf" : "xlsx");
        return Response.ok(bytes, pdf ? "application/pdf" : XLSX)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .build();
    }

    private UUID uuid(String v) {
        return (v != null && !v.isBlank()) ? UUID.fromString(v) : null;
    }

    /** Lectura del reporte: gestión/edición + monitoreo (solo lectura). */
    private void guard() {
        securityUtils.requireAnyRole(
                "ADMIN", "SECRETARIA", "COORDINADOR", "COORD_PROG", "PERS_ADMIN",
                "DECANO", "VICEDECANO", "JEFE_UPG", "COORD_SEC");
    }
}
