package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.DictamenJuradoInforme;

import java.util.Optional;
import java.util.UUID;

public interface DictamenJuradoInformeRepository {
    DictamenJuradoInforme save(DictamenJuradoInforme dictamen);

    Optional<DictamenJuradoInforme> buscarPorTesisId(UUID tesisId);
}
