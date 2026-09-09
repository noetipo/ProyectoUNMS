package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.DictamenExpedito;
import unmsm.edu.pe.tesis.domain.repositories.DictamenExpeditoRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DictamenExpeditoRepositoryImpl
        implements DictamenExpeditoRepository, PanacheRepositoryBase<DictamenExpedito, UUID> {

    @Override
    public DictamenExpedito save(DictamenExpedito dictamen) {
        if (dictamen.getId() == null) {
            persist(dictamen);
            return dictamen;
        }
        return getEntityManager().merge(dictamen);
    }

    @Override
    public Optional<DictamenExpedito> buscarPorTesisId(UUID tesisId) {
        return find("tesisId = ?1 and active = true", tesisId).firstResultOptional();
    }
}
