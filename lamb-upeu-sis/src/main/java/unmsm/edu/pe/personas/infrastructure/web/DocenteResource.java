package unmsm.edu.pe.personas.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.personas.domain.services.DocenteReportService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;

@Path("/api/docentes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Docentes", description = "Reporte de docentes con carga")
public class DocenteResource {

    @Inject DocenteReportService docenteReportService;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Listar docentes", description = "Listado paginado con filtros y carga (asesorías + jurados)")
    public Response listar(
            @QueryParam("search") String search,
            @QueryParam("grado") String grado,
            @QueryParam("categoria") String categoria,
            @QueryParam("condicion") String condicion,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        var pageResponse = docenteReportService.listar(search, grado, categoria, condicion, page, size);
        return Response.ok(ApiResponse.success("Docentes recuperados", pageResponse)).build();
    }

    @GET
    @Path("/resumen")
    @Operation(summary = "Resumen de docentes", description = "Conteos para las tarjetas")
    public Response resumen() {
        guard();
        return Response.ok(ApiResponse.success("Resumen", docenteReportService.resumen())).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("ADMIN", "SECRETARIA", "COORDINADOR");
    }
}
