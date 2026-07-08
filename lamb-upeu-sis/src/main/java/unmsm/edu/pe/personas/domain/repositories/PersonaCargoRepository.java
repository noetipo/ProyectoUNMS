package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.PersonaCargo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonaCargoRepository {
    List<PersonaCargo> findByPersonaId(UUID personaId);
    List<PersonaCargo> saveAll(List<PersonaCargo> cargos);
    void deleteByPersonaId(UUID personaId);
    /** Persona con el cargo vigente cuyo nombre coincide (p. ej. Director de la Unidad de Posgrado). */
    Optional<PersonaCargo> buscarCargoActualPorNombre(String nombreCargo);
}
