package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.DictamenSustentacion;

import java.util.Optional;
import java.util.UUID;

public interface DictamenSustentacionRepository {
    DictamenSustentacion save(DictamenSustentacion dictamen);

    Optional<DictamenSustentacion> buscarPorTesisId(UUID tesisId);
}
