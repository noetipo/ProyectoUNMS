package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.Cargo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CargoRepository {
    Cargo save(Cargo cargo);
    Optional<Cargo> buscarPorId(UUID id);
    List<Cargo> listar(String search, int page, int size);
    long contar(String search);
    long contarTotal();
    boolean existsByNombre(String nombre);
}
