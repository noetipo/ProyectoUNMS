package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.PersonaGradoAcademico;
import unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PersonaGradoAcademicoRepositoryImpl
        implements PersonaGradoAcademicoRepository, PanacheRepositoryBase<PersonaGradoAcademico, UUID> {

    @Override
    public PersonaGradoAcademico save(PersonaGradoAcademico entity) {
        if (entity.getId() == null) {
            persist(entity);
            return entity;
        }
        return getEntityManager().merge(entity);
    }

    @Override
    public List<PersonaGradoAcademico> saveAll(List<PersonaGradoAcademico> entities) {
        persist(entities.stream());
        return entities;
    }

    @Override
    public List<PersonaGradoAcademico> findByPersonaId(UUID personaId) {
        return list("persona.id = ?1 order by principal desc, createdAt asc", personaId);
    }

    @Override
    public void deleteByPersonaId(UUID personaId) {
        delete("persona.id = ?1", personaId);
    }

    @Override
    public String gradoPrincipal(UUID personaId) {
        return find("persona.id = ?1 and principal = true and active = true", personaId)
                .firstResultOptional()
                .map(g -> g.getGrado() != null ? g.getGrado().name() : null)
                .orElse(null);
    }
}
