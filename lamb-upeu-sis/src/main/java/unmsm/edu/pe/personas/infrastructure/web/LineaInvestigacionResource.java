package unmsm.edu.pe.personas.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.personas.application.dto.LineaInvestigacionRequest;
import unmsm.edu.pe.personas.domain.services.LineaInvestigacionService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.UUID;

@Path("/api/lineas-investigacion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Líneas de investigación", description = "Catálogo de líneas de investigación")
public class LineaInvestigacionResource {

    @Inject LineaInvestigacionService lineaInvestigacionService;
    @Inject SecurityUtils securityUtils;

    @GET
    public Response listar(@QueryParam("search") String search,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size) {
        guardRead();
        return Response.ok(ApiResponse.success("Líneas de investigación recuperadas",
                lineaInvestigacionService.listar(search, page, size))).build();
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") UUID id) {
        guardRead();
        return Response.ok(ApiResponse.success("Línea de investigación recuperada",
                lineaInvestigacionService.obtener(id))).build();
    }

    @POST
    public Response crear(@Valid LineaInvestigacionRequest request) {
        guard();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Línea de investigación creada",
                        lineaInvestigacionService.crear(request))).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") UUID id, @Valid LineaInvestigacionRequest request) {
        guard();
        return Response.ok(ApiResponse.success("Línea de investigación actualizada",
                lineaInvestigacionService.actualizar(id, request))).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@Parameter(description = "ID de la línea de investigación") @PathParam("id") UUID id) {
        guard();
        lineaInvestigacionService.eliminar(id);
        return Response.ok(ApiResponse.success("Línea de investigación desactivada")).build();
    }

    /** Escritura del catálogo: solo gestión. */
    private void guard() {
        securityUtils.requireAnyRole("ADMIN", "SECRETARIA");
    }

    /** Lectura del catálogo: lo consumen varios formularios (coordinador de tema, estudiante de asesoría, etc.). */
    private void guardRead() {
        securityUtils.requireAnyRole("ADMIN", "SECRETARIA", "COORDINADOR", "COORD_PROG",
                "COORD_SEC", "ESTUDIANTE", "DOCENTE");
    }
}
