package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.domain.services.NotificacionService;

/** Notificaciones accionables del usuario autenticado (campanita), según su rol. */
@Path("/api/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificaciones", description = "Notificaciones por rol para la campanita")
public class NotificacionResource {

    @Inject NotificacionService service;

    @GET
    @Operation(summary = "Notificaciones del usuario autenticado")
    public Response listar() {
        return Response.ok(ApiResponse.success("Notificaciones", service.paraUsuarioActual())).build();
    }
}
