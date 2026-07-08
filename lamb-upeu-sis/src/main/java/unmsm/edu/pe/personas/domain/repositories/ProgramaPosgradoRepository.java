package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramaPosgradoRepository {
    List<ProgramaPosgrado> getAll();
    /** Programas de una facultad (para la cascada Facultad → Programa). */
    List<ProgramaPosgrado> getByFacultad(UUID facultadId);
    Optional<ProgramaPosgrado> buscarPorId(UUID id);
    ProgramaPosgrado save(ProgramaPosgrado programa);
    boolean existsByNombre(String nombre);
}
