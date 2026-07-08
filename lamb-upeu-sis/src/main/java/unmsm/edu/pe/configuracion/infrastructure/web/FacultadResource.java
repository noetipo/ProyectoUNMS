package unmsm.edu.pe.configuracion.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.configuracion.application.dto.FacultadRequest;
import unmsm.edu.pe.configuracion.domain.services.FacultadService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.UUID;

@Path("/api/facultades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Facultades", description = "Mantenimiento del catálogo de facultades")
public class FacultadResource {

    @Inject FacultadService facultadService;
    @Inject SecurityUtils securityUtils;

    @GET
    public Response listar(@QueryParam("search") String search,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Facultades recuperadas", facultadService.listar(search, page, size))).build();
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") UUID id) {
        guard();
        return Response.ok(ApiResponse.success("Facultad recuperada", facultadService.obtener(id))).build();
    }

    @POST
    public Response crear(@Valid FacultadRequest request) {
        guard();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Facultad creada", facultadService.crear(request))).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") UUID id, @Valid FacultadRequest request) {
        guard();
        return Response.ok(ApiResponse.success("Facultad actualizada", facultadService.actualizar(id, request))).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") UUID id) {
        guard();
        facultadService.eliminar(id);
        return Response.ok(ApiResponse.success("Facultad desactivada")).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("ADMIN", "COORDINADOR", "SECRETARIA");
    }
}
