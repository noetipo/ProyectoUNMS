package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.DictamenJuradoInforme;
import unmsm.edu.pe.tesis.domain.repositories.DictamenJuradoInformeRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DictamenJuradoInformeRepositoryImpl
        implements DictamenJuradoInformeRepository, PanacheRepositoryBase<DictamenJuradoInforme, UUID> {

    @Override
    public DictamenJuradoInforme save(DictamenJuradoInforme dictamen) {
        if (dictamen.getId() == null) {
            persist(dictamen);
            return dictamen;
        }
        return getEntityManager().merge(dictamen);
    }

    @Override
    public Optional<DictamenJuradoInforme> buscarPorTesisId(UUID tesisId) {
        return find("tesisId = ?1 and active = true", tesisId).firstResultOptional();
    }
}
