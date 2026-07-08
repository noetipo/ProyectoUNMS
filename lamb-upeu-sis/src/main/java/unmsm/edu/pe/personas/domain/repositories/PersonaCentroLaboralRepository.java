package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.PersonaCentroLaboral;

import java.util.List;
import java.util.UUID;

public interface PersonaCentroLaboralRepository {
    List<PersonaCentroLaboral> findByPersonaId(UUID personaId);
    List<PersonaCentroLaboral> saveAll(List<PersonaCentroLaboral> centros);
    void deleteByPersonaId(UUID personaId);
}
