package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.RubricaDefensa;
import unmsm.edu.pe.tesis.domain.repositories.RubricaDefensaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RubricaDefensaRepositoryImpl
        implements RubricaDefensaRepository, PanacheRepositoryBase<RubricaDefensa, UUID> {

    @Override
    public RubricaDefensa save(RubricaDefensa rubrica) {
        if (rubrica.getId() == null) {
            persist(rubrica);
            return rubrica;
        }
        return getEntityManager().merge(rubrica);
    }

    @Override
    public List<RubricaDefensa> porTesis(UUID tesisId) {
        return list("tesisId = ?1 and active = true order by fechaCarga", tesisId);
    }

    @Override
    public Optional<RubricaDefensa> buscarPorTesisYDocente(UUID tesisId, UUID docenteId) {
        return find("tesisId = ?1 and docenteId = ?2 and active = true", tesisId, docenteId).firstResultOptional();
    }

    @Override
    public void eliminarPorTesis(UUID tesisId) {
        for (RubricaDefensa r : porTesis(tesisId)) {
            r.setActive(false);
            getEntityManager().merge(r);
        }
    }
}
