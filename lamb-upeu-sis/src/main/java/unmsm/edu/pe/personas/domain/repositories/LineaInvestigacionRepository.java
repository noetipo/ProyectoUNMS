package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LineaInvestigacionRepository {
    LineaInvestigacion save(LineaInvestigacion linea);
    Optional<LineaInvestigacion> buscarPorId(UUID id);
    List<LineaInvestigacion> listar(String search, int page, int size);
    long contar(String search);
    long contarTotal();
    boolean existsByNombre(String nombre);
}
