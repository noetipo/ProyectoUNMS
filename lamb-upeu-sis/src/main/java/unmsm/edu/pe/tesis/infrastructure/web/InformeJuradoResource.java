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
import unmsm.edu.pe.tesis.application.dto.EvaluarInformeRequest;
import unmsm.edu.pe.tesis.domain.services.InformeJuradoService;

import java.util.UUID;

/** Jurado Informante · Etapa 7: evaluación del informe final. */
@Path("/api/jurado-informe")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Etapa 7 · Jurado Informante", description = "Evaluación del informe final")
public class InformeJuradoResource {

    @Inject InformeJuradoService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Informes finales asignados al miembro del Jurado Informante")
    public Response bandeja() {
        guard();
        return ok("Bandeja recuperada", service.bandeja());
    }

    @GET @Path("/{tesisId}")
    @Operation(summary = "Detalle del informe final + mi evaluación")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return ok("Detalle recuperado", service.detalle(tesisId));
    }

    @POST @Path("/{tesisId}/evaluar")
    @Operation(summary = "Registrar la evaluación del informe final (observar o dar conformidad)")
    public Response evaluar(@PathParam("tesisId") UUID tesisId, EvaluarInformeRequest req) {
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
