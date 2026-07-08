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
import unmsm.edu.pe.tutorias.domain.services.MisTutorandosService;

@Path("/api/mis-tutorandos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mis tutorandos", description = "Panel del tutor: sus estudiantes en tutoría vigente")
public class MisTutorandosResource {

    @Inject MisTutorandosService misTutorandosService;
    @Inject SecurityUtils securityUtils;

    @GET
    @Path("/resumen")
    @Operation(summary = "Resumen del tutor autenticado (nombre, total, cupo)")
    public Response resumen() {
        guard();
        return Response.ok(ApiResponse.success("Resumen", misTutorandosService.resumen())).build();
    }

    @GET
    @Operation(summary = "Tutorandos vigentes del tutor autenticado (búsqueda + paginación)")
    public Response tutorandos(
            @QueryParam("buscar") String buscar,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Tutorandos recuperados",
                misTutorandosService.tutorandos(buscar, page, size))).build();
    }

    /** Solo el tutor (rol PROF_TUTOR) accede a su propio panel. */
    private void guard() {
        securityUtils.requireAnyRole("PROF_TUTOR");
    }
}
