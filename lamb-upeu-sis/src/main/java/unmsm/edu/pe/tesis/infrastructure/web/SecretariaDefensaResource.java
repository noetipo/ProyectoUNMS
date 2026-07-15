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
import unmsm.edu.pe.tesis.domain.services.SecretariaDefensaService;

import java.util.UUID;

/** Secretaría · Etapa 5 (Defensa) — bandeja de expedientes recibidos y comunicación al Coordinador. */
@Path("/api/secretaria/defensa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fase 5 · Secretaría (Defensa)", description = "Recepción del expediente y comunicación al Coordinador")
public class SecretariaDefensaResource {

    @Inject SecretariaDefensaService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Bandeja de solicitudes de aprobación recibidas")
    public Response bandeja(@QueryParam("buscar") String buscar,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja recuperada", service.bandeja(buscar, page, size))).build();
    }

    @POST
    @Path("/{tesisId}/recibir")
    @Operation(summary = "Recibir el expediente y comunicar al Coordinador del Programa")
    public Response recibir(@PathParam("tesisId") UUID tesisId) {
        guard();
        service.recibir(tesisId);
        return Response.ok(ApiResponse.success("Expediente recibido y comunicado al Coordinador")).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN");
    }
}
