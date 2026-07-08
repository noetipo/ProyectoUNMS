package unmsm.edu.pe.security.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import unmsm.edu.pe.security.application.dto.ParentModuleDTO;
import unmsm.edu.pe.security.application.dto.ParentModuleRequestDto;
import unmsm.edu.pe.security.domain.services.ParentModuleService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/parent-module")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ParentModuleResource {

    @Inject
    ParentModuleService parentModuleService;

    @GET
    public Response listPaginate(
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("10") Integer size,
            @QueryParam("name") String name) {

        Map<String, Object> response = parentModuleService.listPaginate(page, size, name);
        return Response.ok(response).build();
    }

    @GET
    @Path("/list")
    public Response list(@QueryParam("name") String name) {
        List<ParentModuleDTO> parentModules = parentModuleService.list(name);
        return Response.ok(parentModules).build();
    }


    @POST
    public Response save(@Valid ParentModuleRequestDto requestDto) {
        ParentModuleDTO saved = parentModuleService.save(requestDto);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") UUID id) {
        ParentModuleDTO parentModule = parentModuleService.findById(id);
        return Response.ok(parentModule).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(
            @PathParam("id") UUID id,
            @Valid ParentModuleRequestDto requestDto) {

        ParentModuleDTO updated = parentModuleService.update(id, requestDto);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        parentModuleService.delete(id);
        Map<String, Object> response = parentModuleService.listPaginate(0, 20, null);
        return Response.ok(response).build();
    }
}
