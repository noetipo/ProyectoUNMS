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
import unmsm.edu.pe.tesis.domain.services.TutorProyectoService;

import java.util.UUID;

/** Supervisión del TUTOR sobre los proyectos de sus tutorandos (Etapa 4, solo lectura). */
@Path("/api/tutor/proyectos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fase 4 · Supervisión del tutor", description = "El tutor supervisa el proyecto según el plan de actividades")
public class TutorProyectoResource {

    @Inject TutorProyectoService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Bandeja de proyectos a supervisar (tutorandos del tutor autenticado)")
    public Response bandeja(@QueryParam("buscar") String buscar,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja recuperada", service.bandeja(buscar, page, size))).build();
    }

    @GET
    @Path("/{tesisId}")
    @Operation(summary = "Detalle del proyecto para supervisión (solo lectura)")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return Response.ok(ApiResponse.success("Proyecto recuperado", service.detalle(tesisId))).build();
    }

    private void guard() {
        // Solo el tutor supervisa (no aprueba). Rol PROF_TUTOR.
        securityUtils.requireAnyRole("PROF_TUTOR");
    }
}
