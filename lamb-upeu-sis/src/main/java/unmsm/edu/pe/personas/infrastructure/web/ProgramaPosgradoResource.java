package unmsm.edu.pe.personas.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.personas.domain.services.ProgramaPosgradoService;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.UUID;

@Path("/api/programas-posgrado")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Programas Posgrado", description = "Catálogo de programas de posgrado")
public class ProgramaPosgradoResource {

    @Inject ProgramaPosgradoService programaService;

    @GET
    @Operation(summary = "Listar programas de posgrado (opcional: filtrar por facultad)")
    public Response list(@QueryParam("facultadId") UUID facultadId) {
        return Response.ok(ApiResponse.success("Programas recuperados", programaService.list(facultadId))).build();
    }
}
