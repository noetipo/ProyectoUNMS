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
import unmsm.edu.pe.tesis.domain.services.ExpedienteTesisService;

import java.util.UUID;

/** Expediente de tesis: línea de tiempo de las 8 etapas del proceso de titulación. */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Expediente de tesis", description = "Línea de tiempo del proceso de titulación")
public class ExpedienteResource {

    @Inject ExpedienteTesisService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Path("/mi-expediente")
    @Operation(summary = "Expediente del estudiante autenticado")
    public Response miExpediente() {
        securityUtils.requireAnyRole("ESTUDIANTE");
        return Response.ok(ApiResponse.success("Expediente recuperado", service.miExpediente())).build();
    }

    @GET
    @Path("/expedientes/{tesisId}")
    @Operation(summary = "Expediente de una tesis (secretaría / admin / coordinador)")
    public Response expediente(@PathParam("tesisId") UUID tesisId) {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN", "COORDINADOR", "COORD_PROG", "COORD_SEC");
        return Response.ok(ApiResponse.success("Expediente recuperado", service.expediente(tesisId))).build();
    }
}
