package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.application.dto.DesignarJuradoInformeRequest;
import unmsm.edu.pe.tesis.application.dto.DesignarRevisoresRequest;
import unmsm.edu.pe.tesis.domain.services.CoordinadorProyectoService;

import java.util.UUID;

/** Coordinador · Etapa 5: recepción del expediente y designación de revisores del proyecto. */
@Path("/api/coordinador/proyectos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Etapa 5 · Coordinador", description = "Designación de revisores del proyecto")
public class CoordinadorProyectoResource {

    @Inject CoordinadorProyectoService service;

    @GET
    @Operation(summary = "Bandeja del coordinador: proyectos con expediente recepcionado")
    public Response bandeja(@QueryParam("buscar") String buscar,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("20") int size) {
        return Response.ok(ApiResponse.success("Bandeja recuperada", service.bandeja(buscar, page, size))).build();
    }

    @GET @Path("/{tesisId}/docentes")
    @Operation(summary = "Docentes de la línea de investigación de la tesis, seleccionables como revisores")
    public Response docentes(@PathParam("tesisId") UUID tesisId) {
        return Response.ok(ApiResponse.success("Docentes", service.docentesDisponibles(tesisId))).build();
    }

    @GET @Path("/{tesisId}/revisores")
    @Operation(summary = "Revisores designados de un proyecto")
    public Response revisores(@PathParam("tesisId") UUID tesisId) {
        return Response.ok(ApiResponse.success("Revisores", service.revisores(tesisId))).build();
    }

    @POST @Path("/{tesisId}/revisores")
    @Operation(summary = "Designar los 2 revisores del proyecto")
    public Response designar(@PathParam("tesisId") UUID tesisId, DesignarRevisoresRequest req) {
        service.designarRevisores(tesisId, req);
        return Response.ok(ApiResponse.success("Revisores designados")).build();
    }

    @GET @Path("/{tesisId}/defensa")
    @Operation(summary = "Información de la defensa programada (jurado + fecha) — solo lectura")
    public Response defensa(@PathParam("tesisId") UUID tesisId) {
        return Response.ok(ApiResponse.success("Defensa", service.defensa(tesisId))).build();
    }

    // La programación de la defensa la realiza ahora la Secretaría (SecretariaDefensaResource).

    @GET @Path("/{tesisId}/jurado-informe")
    @Operation(summary = "Miembros del Jurado Informante designados")
    public Response juradoInforme(@PathParam("tesisId") UUID tesisId) {
        return Response.ok(ApiResponse.success("Jurado Informante", service.juradoInforme(tesisId))).build();
    }

    @POST @Path("/{tesisId}/jurado-informe")
    @Operation(summary = "Designar los 3 miembros del Jurado Informante del informe final")
    public Response designarJuradoInforme(@PathParam("tesisId") UUID tesisId, DesignarJuradoInformeRequest req) {
        service.designarJuradoInforme(tesisId, req);
        return Response.ok(ApiResponse.success("Jurado Informante designado")).build();
    }

    @GET @Path("/{tesisId}/jurado-sustentacion")
    @Operation(summary = "Miembros del Jurado de Sustentación designados")
    public Response juradoSustentacion(@PathParam("tesisId") UUID tesisId) {
        return Response.ok(ApiResponse.success("Jurado de Sustentación", service.juradoSustentacion(tesisId))).build();
    }

    @POST @Path("/{tesisId}/jurado-sustentacion")
    @Operation(summary = "Designar los 3 miembros del Jurado de Sustentación")
    public Response designarJuradoSustentacion(@PathParam("tesisId") UUID tesisId, DesignarJuradoInformeRequest req) {
        service.designarJuradoSustentacion(tesisId, req);
        return Response.ok(ApiResponse.success("Jurado de Sustentación designado")).build();
    }
}
