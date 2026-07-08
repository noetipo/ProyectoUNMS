package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.CentroLaboral;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CentroLaboralRepository {
    CentroLaboral save(CentroLaboral centro);
    Optional<CentroLaboral> buscarPorId(UUID id);
    List<CentroLaboral> listar(String search, int page, int size);
    long contar(String search);
    long contarTotal();
    boolean existsByNombre(String nombre);
}
