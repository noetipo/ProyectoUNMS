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
import unmsm.edu.pe.tesis.application.dto.SugerirAsesorRequest;
import unmsm.edu.pe.tesis.domain.services.SugerenciaAsesorService;

import java.util.UUID;

/**
 * Sugerencias de asesor que el tutor hace a sus tutorandos (panel del tutor).
 * Solo el rol PROF_TUTOR y únicamente para SUS propios tutorandos.
 */
@Path("/api/tutor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tutor · Sugerencias de asesor", description = "El tutor sugiere asesores a sus tutorandos")
public class SugerenciaAsesorResource {

    @Inject SugerenciaAsesorService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Path("/estudiantes/{estudianteId}/sugerencias")
    @Operation(summary = "Sugerencias de asesor de un tutorando (del tutor autenticado)")
    public Response listar(@PathParam("estudianteId") UUID estudianteId) {
        guard();
        return Response.ok(ApiResponse.success("Sugerencias recuperadas",
                service.listarDeMiTutorando(estudianteId))).build();
    }

    @GET
    @Path("/estudiantes/{estudianteId}/asesores-candidatos")
    @Operation(summary = "Docentes candidatos a asesor (filtrados por la línea del tema del tutorando)")
    public Response candidatos(@PathParam("estudianteId") UUID estudianteId) {
        guard();
        return Response.ok(ApiResponse.success("Candidatos recuperados",
                service.candidatosDeMiTutorando(estudianteId))).build();
    }

    @POST
    @Path("/estudiantes/{estudianteId}/sugerencias")
    @Operation(summary = "Sugerir un asesor a un tutorando")
    public Response sugerir(@PathParam("estudianteId") UUID estudianteId, @Valid SugerirAsesorRequest request) {
        guard();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Asesor sugerido", service.sugerir(estudianteId, request)))
                .build();
    }

    @DELETE
    @Path("/sugerencias/{sugerenciaId}")
    @Operation(summary = "Quitar una sugerencia de asesor")
    public Response quitar(@PathParam("sugerenciaId") UUID sugerenciaId) {
        guard();
        service.quitar(sugerenciaId);
        return Response.ok(ApiResponse.success("Sugerencia eliminada")).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("PROF_TUTOR");
    }
}
