package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.DictamenExpedito;

import java.util.Optional;
import java.util.UUID;

public interface DictamenExpeditoRepository {
    DictamenExpedito save(DictamenExpedito dictamen);

    Optional<DictamenExpedito> buscarPorTesisId(UUID tesisId);
}
