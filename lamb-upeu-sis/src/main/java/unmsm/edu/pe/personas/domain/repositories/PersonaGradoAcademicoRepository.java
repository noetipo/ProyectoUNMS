package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.PersonaGradoAcademico;

import java.util.List;
import java.util.UUID;

public interface PersonaGradoAcademicoRepository {
    PersonaGradoAcademico save(PersonaGradoAcademico entity);
    List<PersonaGradoAcademico> saveAll(List<PersonaGradoAcademico> entities);
    List<PersonaGradoAcademico> findByPersonaId(UUID personaId);
    void deleteByPersonaId(UUID personaId);

    /** Nombre del grado marcado como principal para la persona, o null. */
    String gradoPrincipal(UUID personaId);
}
