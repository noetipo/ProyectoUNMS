package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.application.dto.RegistrarAvanceRequest;
import unmsm.edu.pe.tesis.domain.services.AsesorEjecucionService;

import java.util.UUID;

/** Asesor · Etapa 6: ejecución de la tesis (avances con rúbrica + aprobación del informe final). */
@Path("/api/asesor/ejecucion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Etapa 6 · Ejecución (asesor)", description = "Rúbrica de avances y aprobación del informe final")
public class AsesorEjecucionResource {

    @Inject AsesorEjecucionService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Proyectos del asesor en ejecución")
    public Response bandeja() {
        guard();
        return ok("Bandeja recuperada", service.bandeja());
    }

    @GET @Path("/{tesisId}")
    @Operation(summary = "Detalle de ejecución: plan, avances e informe final")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return ok("Detalle recuperado", service.detalle(tesisId));
    }

    @POST @Path("/{tesisId}/avances")
    @Operation(summary = "Registrar evaluación de avance con la rúbrica de avances")
    public Response registrarAvance(@PathParam("tesisId") UUID tesisId, RegistrarAvanceRequest req) {
        guard();
        service.registrarAvance(tesisId, req);
        return ok("Avance registrado", null);
    }

    @POST @Path("/{tesisId}/informe-final/aprobar")
    @Operation(summary = "Aprobar el informe final (carta), con el plan 100% ejecutado")
    public Response aprobarInforme(@PathParam("tesisId") UUID tesisId) {
        guard();
        service.aprobarInformeFinal(tesisId);
        return ok("Informe final aprobado", null);
    }

    private Response ok(String msg, Object data) {
        return Response.ok(data != null ? ApiResponse.success(msg, data) : ApiResponse.success(msg)).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("ASESOR", "ADMIN");
    }
}
