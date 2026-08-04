package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.domain.services.SeguimientoService;

import java.util.UUID;

/**
 * Tablero de seguimiento de doctorandos: en qué etapa va cada uno y qué acción lo tiene
 * detenido. Es solo lectura y lo usan la Secretaría (su pantalla principal de seguimiento),
 * la Coordinación y el Admin.
 */
@Path("/api/seguimiento")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Seguimiento de doctorandos", description = "Tablero de etapas y pendientes (secretaría/coordinación)")
public class SeguimientoResource {

    @Inject SeguimientoService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Tablero de seguimiento: estadística por etapa + alumnos con su pendiente")
    public Response tablero(@QueryParam("programaId") UUID programaId,
                            @QueryParam("buscar") String buscar) {
        guard();
        return Response.ok(ApiResponse.success("Seguimiento", service.tablero(programaId, buscar))).build();
    }

    @GET
    @Path("/excel")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Exporta el tablero a Excel con los filtros aplicados")
    public Response excel(@QueryParam("buscar") String buscar,
                          @QueryParam("etapa") Integer etapa,
                          @QueryParam("responsable") String responsable,
                          @QueryParam("programa") String programa) {
        guard();
        byte[] xlsx = service.excel(buscar, etapa, responsable, programa);
        return Response.ok(xlsx)
                .header("Content-Disposition", "attachment; filename=\"seguimiento-doctorandos.xlsx\"")
                .build();
    }

    private void guard() {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN", "COORDINADOR", "COORD_PROG", "COORD_SEC");
    }
}
