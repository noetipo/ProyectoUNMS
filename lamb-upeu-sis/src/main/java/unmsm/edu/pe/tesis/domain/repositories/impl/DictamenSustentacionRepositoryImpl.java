package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.DictamenSustentacion;
import unmsm.edu.pe.tesis.domain.repositories.DictamenSustentacionRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DictamenSustentacionRepositoryImpl
        implements DictamenSustentacionRepository, PanacheRepositoryBase<DictamenSustentacion, UUID> {

    @Override
    public DictamenSustentacion save(DictamenSustentacion dictamen) {
        if (dictamen.getId() == null) {
            persist(dictamen);
            return dictamen;
        }
        return getEntityManager().merge(dictamen);
    }

    @Override
    public Optional<DictamenSustentacion> buscarPorTesisId(UUID tesisId) {
        return find("tesisId = ?1 and active = true", tesisId).firstResultOptional();
    }
}
