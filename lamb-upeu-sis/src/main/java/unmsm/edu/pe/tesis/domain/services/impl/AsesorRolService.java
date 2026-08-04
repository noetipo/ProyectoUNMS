package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.entities.UserRoleAssignment;
import unmsm.edu.pe.security.domain.repositories.RoleRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;

import java.util.UUID;

/**
 * Otorga el rol ASESOR a un docente cuando es designado como asesor de una tesis. Es idempotente
 * (no duplica el rol) y tolerante a datos faltantes. Sin este rol, el docente no puede acceder a la
 * bandeja de revisión del proyecto (guard {@code requireAnyRole("ASESOR")}).
 *
 * <p>Se debe invocar en TODA ruta donde un docente pase a ser asesor: al aceptar la solicitud de
 * asesoría (designación) y al materializar el proyecto con su asesor (auto-repara datos previos).</p>
 */
@ApplicationScoped
public class AsesorRolService {

    @Inject PersonaRepository personaRepository;
    @Inject RoleRepository roleRepository;
    @Inject UserRoleAssignmentRepository userRoleRepository;

    /** Otorga el rol ASESOR al docente (por su personaId) si aún no lo tiene. No-op si falta algún dato. */
    public void otorgarRolAsesor(UUID docentePersonaId) {
        if (docentePersonaId == null) return;
        Persona persona = personaRepository.buscarPorId(docentePersonaId).orElse(null);
        if (persona == null || persona.getUser() == null) return;
        User user = persona.getUser();
        Role rolAsesor = roleRepository.findFirstByCodeOrderByCreatedAtAsc("ASESOR").orElse(null);
        if (rolAsesor == null) return;
        boolean yaTiene = userRoleRepository.findRoleCodesByUserId(user.getId()).contains("ASESOR");
        if (!yaTiene) {
            userRoleRepository.save(UserRoleAssignment.builder()
                    .user(user).role(rolAsesor).assigned(true).build());
        }
    }
}
