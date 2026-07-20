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
import unmsm.edu.pe.tesis.application.dto.EvaluarRevisorRequest;
import unmsm.edu.pe.tesis.domain.services.RevisorProyectoService;

import java.util.UUID;

/** Revisor (Jurado Informante) · Etapa 5, paso 3: evaluación del proyecto con rúbrica. */
@Path("/api/revisor/proyectos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Etapa 5 · Revisor", description = "Evaluación del proyecto con rúbrica")
public class RevisorProyectoResource {

    @Inject RevisorProyectoService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Proyectos asignados al revisor")
    public Response bandeja() {
        guard();
        return ok("Bandeja recuperada", service.bandeja());
    }

    @GET @Path("/{tesisId}")
    @Operation(summary = "Detalle del proyecto + rúbrica del revisor")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return ok("Detalle recuperado", service.detalle(tesisId));
    }

    @POST @Path("/{tesisId}/evaluar")
    @Operation(summary = "Registrar la evaluación con rúbrica (observar o dar conformidad)")
    public Response evaluar(@PathParam("tesisId") UUID tesisId, EvaluarRevisorRequest req) {
        guard();
        service.evaluar(tesisId, req);
        return ok("Evaluación registrada", null);
    }

    private Response ok(String msg, Object data) {
        return Response.ok(data != null ? ApiResponse.success(msg, data) : ApiResponse.success(msg)).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("DOCENTE", "ADMIN");
    }
}
