package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.application.dto.ConformidadSeccionRequest;
import unmsm.edu.pe.tesis.application.dto.ObservarItemRequest;
import unmsm.edu.pe.tesis.domain.services.AsesorProyectoService;

import java.util.UUID;

/** Revisión del proyecto por el ASESOR (docente): bandeja, observar y aprobar por ítem. */
@Path("/api/asesor/proyectos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fase 4 · Revisión del asesor", description = "El asesor revisa y observa el proyecto por ítem")
public class AsesorProyectoResource {

    @Inject AsesorProyectoService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Bandeja de proyectos a revisar del asesor autenticado")
    public Response bandeja(@QueryParam("estado") String estado,
                            @QueryParam("buscar") String buscar,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja recuperada",
                service.bandeja(estado, buscar, page, size))).build();
    }

    @GET
    @Path("/{tesisId}")
    @Operation(summary = "Detalle del proyecto (editor en modo revisión)")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return Response.ok(ApiResponse.success("Proyecto recuperado", service.detalle(tesisId))).build();
    }

    @POST
    @Path("/{tesisId}/observar")
    @Operation(summary = "Observar un ítem del proyecto")
    public Response observar(@PathParam("tesisId") UUID tesisId, @Valid ObservarItemRequest req) {
        guard();
        service.observarItem(tesisId, req);
        return Response.ok(ApiResponse.success("Ítem observado")).build();
    }

    @POST
    @Path("/{tesisId}/campos/{campo}/conforme")
    @Operation(summary = "Dar conformidad a un ítem corregido")
    public Response conforme(@PathParam("tesisId") UUID tesisId, @PathParam("campo") String campo) {
        guard();
        service.darConformidad(tesisId, campo);
        return Response.ok(ApiResponse.success("Ítem con conformidad")).build();
    }

    @POST
    @Path("/{tesisId}/conformidad-seccion")
    @Operation(summary = "Dar conformidad a todos los ítems de una sección (flujo por bloque)")
    public Response conformeSeccion(@PathParam("tesisId") UUID tesisId, ConformidadSeccionRequest req) {
        guard();
        service.darConformidadSeccion(tesisId, req != null ? req.getCampos() : null);
        return Response.ok(ApiResponse.success("Sección con conformidad")).build();
    }

    @POST
    @Path("/{tesisId}/carta-opinion")
    @Operation(summary = "Emitir la carta de opinión favorable (todos los ítems conformes)")
    public Response cartaOpinion(@PathParam("tesisId") UUID tesisId) {
        guard();
        service.emitirCartaOpinion(tesisId);
        return Response.ok(ApiResponse.success("Carta de opinión favorable emitida")).build();
    }

    private void guard() {
        // Solo el rol ASESOR revisa/aprueba el proyecto (los tutores NO participan aquí).
        securityUtils.requireAnyRole("ASESOR");
    }
}
