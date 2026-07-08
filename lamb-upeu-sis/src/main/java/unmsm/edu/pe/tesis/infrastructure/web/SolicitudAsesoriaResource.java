package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.application.dto.CrearSolicitudRequest;
import unmsm.edu.pe.tesis.application.dto.ResponderSolicitudRequest;
import unmsm.edu.pe.tesis.domain.services.SolicitudAsesoriaService;

import java.util.UUID;

@Path("/api/solicitudes-asesoria")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Solicitudes de asesoría", description = "Fase 1 · Dictamen asesor (solicitud → aceptar/rechazar)")
public class SolicitudAsesoriaResource {

    @Inject SolicitudAsesoriaService solicitudService;
    @Inject SecurityUtils securityUtils;

    // ── Estudiante ─────────────────────────────────────────────────────────

    @POST
    @Operation(summary = "Registrar solicitud de asesoría (estudiante)")
    public Response crear(@Valid CrearSolicitudRequest request) {
        guardEstudiante();
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Solicitud registrada", solicitudService.crear(request)))
                .build();
    }

    @POST
    @Path("/{id}/cancelar")
    @Operation(summary = "Cancelar una solicitud PENDIENTE (estudiante)")
    public Response cancelar(@PathParam("id") UUID id) {
        guardEstudiante();
        return Response.ok(ApiResponse.success("Solicitud cancelada", solicitudService.cancelar(id))).build();
    }

    // ── Docente ────────────────────────────────────────────────────────────

    @GET
    @Path("/bandeja")
    @Operation(summary = "Bandeja de solicitudes (docente)")
    public Response bandeja(
            @QueryParam("estado") String estado,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guardDocente();
        return Response.ok(ApiResponse.success("Bandeja recuperada",
                solicitudService.bandeja(estado, page, size))).build();
    }

    @POST
    @Path("/{id}/responder")
    @Operation(summary = "Aceptar o rechazar una solicitud (docente)")
    public Response responder(@PathParam("id") UUID id, @Valid ResponderSolicitudRequest request) {
        guardDocente();
        return Response.ok(ApiResponse.success("Solicitud respondida",
                solicitudService.responder(id, request))).build();
    }

    // ── guards ─────────────────────────────────────────────────────────────

    private void guardEstudiante() {
        securityUtils.requireAnyRole("ESTUDIANTE");
    }

    private void guardDocente() {
        securityUtils.requireAnyRole("DOCENTE");
    }
}
