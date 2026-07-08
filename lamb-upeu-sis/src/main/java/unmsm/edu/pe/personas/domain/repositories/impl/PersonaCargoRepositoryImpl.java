package unmsm.edu.pe.personas.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.PersonaCargo;
import unmsm.edu.pe.personas.domain.repositories.PersonaCargoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PersonaCargoRepositoryImpl implements PersonaCargoRepository, PanacheRepositoryBase<PersonaCargo, UUID> {

    @Override
    public List<PersonaCargo> findByPersonaId(UUID personaId) {
        return list("persona.id = ?1", Sort.by("fechaInicio").descending(), personaId);
    }

    @Override
    public Optional<PersonaCargo> buscarCargoActualPorNombre(String nombreCargo) {
        if (nombreCargo == null || nombreCargo.isBlank()) {
            return Optional.empty();
        }
        return find("actual = true and active = true and lower(cargo.nombre) = ?1",
                nombreCargo.trim().toLowerCase()).firstResultOptional();
    }

    @Override
    public List<PersonaCargo> saveAll(List<PersonaCargo> cargos) {
        persist(cargos);
        return cargos;
    }

    @Override
    public void deleteByPersonaId(UUID personaId) {
        delete("persona.id = ?1", personaId);
    }
}
