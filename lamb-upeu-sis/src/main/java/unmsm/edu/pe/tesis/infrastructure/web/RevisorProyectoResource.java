package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.application.dto.EvaluarRevisorRequest;
import unmsm.edu.pe.tesis.application.dto.ObservarItemRequest;
import unmsm.edu.pe.tesis.domain.services.RevisorProyectoService;

import java.util.UUID;

/** Revisor (Jurado Informante) · Etapa 5, paso 3: evaluación del proyecto con rúbrica. */
@Path("/api/revisor/proyectos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Etapa 5 · Revisor", description = "Evaluación del proyecto con rúbrica")
public class RevisorProyectoResource {

    @Inject RevisorProyectoService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Proyectos asignados al revisor")
    public Response bandeja() {
        guard();
        return ok("Bandeja recuperada", service.bandeja());
    }

    @GET @Path("/{tesisId}")
    @Operation(summary = "Detalle del proyecto + rúbrica del revisor")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return ok("Detalle recuperado", service.detalle(tesisId));
    }

    @GET @Path("/{tesisId}/rubrica")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar la rúbrica oficial (Word) subida por Secretaría — solo referencia")
    public Response descargarRubrica(@PathParam("tesisId") UUID tesisId) {
        guard();
        var a = service.descargarRubrica(tesisId);
        String ct = a.contentType() != null ? a.contentType()
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String name = a.nombreOriginal() != null ? a.nombreOriginal() : "rubrica.docx";
        return Response.ok(a.contenido(), ct)
                .header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                .build();
    }

    @GET @Path("/{tesisId}/rubrica-llenada")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar la rúbrica llenada (Word) con los puntajes registrados por el revisor")
    public Response descargarRubricaLlenada(@PathParam("tesisId") UUID tesisId) {
        guard();
        var a = service.descargarRubricaLlenada(tesisId);
        return Response.ok(a.contenido(), a.contentType())
                .header("Content-Disposition", "attachment; filename=\"" + a.nombreOriginal() + "\"")
                .build();
    }

    @GET @Path("/{tesisId}/proyecto-pdf")
    @Produces("application/pdf")
    @Operation(summary = "PDF del proyecto para el visor del revisor (solo lectura)")
    public Response proyectoPdf(@PathParam("tesisId") UUID tesisId) {
        guard();
        var a = service.descargarProyectoPdf(tesisId);
        return Response.ok(a.contenido(), "application/pdf")
                .header("Content-Disposition", "inline; filename=\"proyecto.pdf\"")
                .build();
    }

    @POST @Path("/{tesisId}/observar-item")
    @Operation(summary = "Observar un ítem/campo del proyecto (informe de revisor)")
    public Response observarItem(@PathParam("tesisId") UUID tesisId, ObservarItemRequest req) {
        guard();
        service.observarItem(tesisId, req);
        return ok("Observación registrada", null);
    }

    @POST @Path("/{tesisId}/items/{campo}/conformidad")
    @Operation(summary = "Validar la corrección de un ítem (dar conformidad al ítem)")
    public Response conformidadItem(@PathParam("tesisId") UUID tesisId, @PathParam("campo") String campo) {
        guard();
        service.darConformidadItem(tesisId, campo);
        return ok("Corrección validada", null);
    }

    @POST @Path("/{tesisId}/evaluar")
    @Operation(summary = "Registrar la evaluación con rúbrica (observar o dar conformidad)")
    public Response evaluar(@PathParam("tesisId") UUID tesisId, EvaluarRevisorRequest req) {
        guard();
        service.evaluar(tesisId, req);
        return ok("Evaluación registrada", null);
    }

    private Response ok(String msg, Object data) {
        return Response.ok(data != null ? ApiResponse.success(msg, data) : ApiResponse.success(msg)).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("DOCENTE", "ADMIN");
    }
}
