package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.DictamenAprobacion;
import unmsm.edu.pe.tesis.domain.repositories.DictamenAprobacionRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DictamenAprobacionRepositoryImpl
        implements DictamenAprobacionRepository, PanacheRepositoryBase<DictamenAprobacion, UUID> {

    @Override
    public DictamenAprobacion save(DictamenAprobacion dictamen) {
        if (dictamen.getId() == null) {
            persist(dictamen);
            return dictamen;
        }
        return getEntityManager().merge(dictamen);
    }

    @Override
    public Optional<DictamenAprobacion> buscarPorTesisId(UUID tesisId) {
        return find("tesisId = ?1 and active = true", tesisId).firstResultOptional();
    }
}
