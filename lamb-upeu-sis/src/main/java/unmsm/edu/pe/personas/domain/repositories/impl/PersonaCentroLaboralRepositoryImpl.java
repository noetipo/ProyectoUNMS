package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.PersonaCentroLaboral;
import unmsm.edu.pe.personas.domain.repositories.PersonaCentroLaboralRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PersonaCentroLaboralRepositoryImpl
        implements PersonaCentroLaboralRepository, PanacheRepositoryBase<PersonaCentroLaboral, UUID> {

    @Override
    public List<PersonaCentroLaboral> findByPersonaId(UUID personaId) {
        return list("persona.id = ?1", Sort.by("fechaInicio").descending(), personaId);
    }

    @Override
    public List<PersonaCentroLaboral> saveAll(List<PersonaCentroLaboral> centros) {
        persist(centros);
        return centros;
    }

    @Override
    public void deleteByPersonaId(UUID personaId) {
        delete("persona.id = ?1", personaId);
    }
}
