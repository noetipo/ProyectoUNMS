package unmsm.edu.pe.personas.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.personas.application.dto.CargoRequest;
import unmsm.edu.pe.personas.domain.services.CargoService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.UUID;

@Path("/api/cargos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cargos", description = "Mantenimiento del catálogo de cargos")
public class CargoResource {

    @Inject CargoService cargoService;
    @Inject SecurityUtils securityUtils;

    @GET
    public Response listar(@QueryParam("search") String search,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Cargos recuperados", cargoService.listar(search, page, size))).build();
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") UUID id) {
        guard();
        return Response.ok(ApiResponse.success("Cargo recuperado", cargoService.obtener(id))).build();
    }

    @POST
    public Response crear(@Valid CargoRequest request) {
        guard();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Cargo creado", cargoService.crear(request))).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") UUID id, @Valid CargoRequest request) {
        guard();
        return Response.ok(ApiResponse.success("Cargo actualizado", cargoService.actualizar(id, request))).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@Parameter(description = "ID del cargo") @PathParam("id") UUID id) {
        guard();
        cargoService.eliminar(id);
        return Response.ok(ApiResponse.success("Cargo desactivado")).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("ADMIN", "SECRETARIA");
    }
}
