package unmsm.edu.pe.configuracion.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.configuracion.application.dto.LemaAnualRequest;
import unmsm.edu.pe.configuracion.domain.services.LemaAnualService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.UUID;

@Path("/api/lemas-anuales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Lemas anuales", description = "Mantenimiento del nombre/lema oficial del año")
public class LemaAnualResource {

    @Inject LemaAnualService lemaService;
    @Inject SecurityUtils securityUtils;

    @GET
    public Response listar(@QueryParam("search") String search,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Lemas recuperados", lemaService.listar(search, page, size))).build();
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") UUID id) {
        guard();
        return Response.ok(ApiResponse.success("Lema recuperado", lemaService.obtener(id))).build();
    }

    @POST
    public Response crear(@Valid LemaAnualRequest request) {
        guard();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Lema creado", lemaService.crear(request))).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") UUID id, @Valid LemaAnualRequest request) {
        guard();
        return Response.ok(ApiResponse.success("Lema actualizado", lemaService.actualizar(id, request))).build();
    }

    @POST
    @Path("/{id}/activar")
    public Response activar(@PathParam("id") UUID id) {
        guard();
        return Response.ok(ApiResponse.success("Lema activado", lemaService.activar(id))).build();
    }

    @POST
    @Path("/{id}/desactivar")
    public Response desactivar(@PathParam("id") UUID id) {
        guard();
        return Response.ok(ApiResponse.success("Lema desactivado", lemaService.desactivar(id))).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("ADMIN", "COORDINADOR", "SECRETARIA");
    }
}
