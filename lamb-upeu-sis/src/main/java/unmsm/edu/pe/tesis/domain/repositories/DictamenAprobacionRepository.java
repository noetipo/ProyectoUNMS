package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.DictamenAprobacion;

import java.util.Optional;
import java.util.UUID;

public interface DictamenAprobacionRepository {
    DictamenAprobacion save(DictamenAprobacion dictamen);

    Optional<DictamenAprobacion> buscarPorTesisId(UUID tesisId);
}
