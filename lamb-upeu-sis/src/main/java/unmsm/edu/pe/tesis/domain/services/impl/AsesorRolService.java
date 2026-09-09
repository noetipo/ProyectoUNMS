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
 * Otorga a un docente el rol que le corresponde cuando el proceso lo designa: ASESOR al aceptar
 * la asesoría, REVISOR al ser designado revisor de un proyecto. Es idempotente (no duplica el rol)
 * y tolerante a datos faltantes. Sin el rol, el docente no ve la bandeja correspondiente
 * (guard {@code requireAnyRole("ASESOR")}) ni se distingue su papel en el sistema.
 *
 * <p>Se debe invocar en TODA ruta donde un docente pase a cumplir el papel: al aceptar la solicitud
 * de asesoría, al materializar el proyecto con su asesor (auto-repara datos previos) y al designar
 * los revisores de un proyecto.</p>
 */
@ApplicationScoped
public class AsesorRolService {

    @Inject PersonaRepository personaRepository;
    @Inject RoleRepository roleRepository;
    @Inject UserRoleAssignmentRepository userRoleRepository;

    /** Otorga el rol ASESOR al docente (por su personaId) si aún no lo tiene. No-op si falta algún dato. */
    public void otorgarRolAsesor(UUID docentePersonaId) {
        otorgar(docentePersonaId, "ASESOR");
    }

    /** Otorga el rol REVISOR al docente designado como revisor de un proyecto. */
    public void otorgarRolRevisor(UUID docentePersonaId) {
        otorgar(docentePersonaId, "REVISOR");
    }

    private void otorgar(UUID docentePersonaId, String codigoRol) {
        if (docentePersonaId == null) return;
        Persona persona = personaRepository.buscarPorId(docentePersonaId).orElse(null);
        if (persona == null || persona.getUser() == null) return;
        User user = persona.getUser();
        Role rol = roleRepository.findFirstByCodeOrderByCreatedAtAsc(codigoRol).orElse(null);
        if (rol == null) return;
        boolean yaTiene = userRoleRepository.findRoleCodesByUserId(user.getId()).contains(codigoRol);
        if (!yaTiene) {
            userRoleRepository.save(UserRoleAssignment.builder()
                    .user(user).role(rol).assigned(true).build());
        }
    }
}
