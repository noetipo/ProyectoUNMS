package unmsm.edu.pe.configuracion.domain.repositories;

import unmsm.edu.pe.configuracion.domain.entities.Facultad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacultadRepository {
    Facultad save(Facultad facultad);
    Optional<Facultad> buscarPorId(UUID id);
    Optional<Facultad> buscarPorCodigo(String codigo);
    List<Facultad> listar(String search, int page, int size);
    long contar(String search);
    boolean existsByNombre(String nombre);
    boolean existsByCodigo(String codigo);
}
