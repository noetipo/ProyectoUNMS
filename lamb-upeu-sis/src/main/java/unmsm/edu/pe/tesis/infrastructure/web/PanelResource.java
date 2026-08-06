package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.domain.services.PanelService;

/**
 * Panel de inicio. Abierto a <b>cualquier usuario autenticado</b>: la parte institucional son solo
 * conteos (sin nombres) y la personal se arma con lo que ese usuario ya puede ver por su rol.
 */
@Path("/api/panel")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Panel", description = "Inicio: lo importante primero según el rol + cifras del programa")
public class PanelResource {

    @Inject PanelService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Panel de inicio del usuario autenticado")
    public Response panel() {
        autenticado();
        return Response.ok(ApiResponse.success("Panel", service.panel())).build();
    }

    /** No hay filtro por rol: basta con estar autenticado (no se expone ningún dato personal ajeno). */
    private void autenticado() {
        if (securityUtils.getCurrentUserIdAsUUID() == null) {
            throw new BusinessException("Usuario no autenticado");
        }
    }
}
