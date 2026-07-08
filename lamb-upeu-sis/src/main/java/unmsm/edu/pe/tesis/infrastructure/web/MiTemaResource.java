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
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.domain.services.CoordinadorTemaService;

import java.util.UUID;

/**
 * Sección "Tema de investigación" del perfil del estudiante (solo lectura).
 * Resuelve la persona desde el token y devuelve su tema (tesis activa) o null.
 * Usa el mismo estado derivado que el coordinador y el tutor.
 */
@Path("/api/mi-perfil/tema")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mi Perfil", description = "Tema de investigación del usuario autenticado")
public class MiTemaResource {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject CoordinadorTemaService coordinadorTemaService;

    @GET
    @Operation(summary = "Mi tema de investigación", description = "Tesis activa del estudiante autenticado (o null)")
    public Response miTema() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        Persona persona = personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
        // El id de estudiante es el mismo persona_id (PK compartida).
        return Response.ok(ApiResponse.success("Tema recuperado",
                coordinadorTemaService.temaDeEstudiante(persona.getId()))).build();
    }
}
