package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;

import java.util.UUID;

@ApplicationScoped
public class AsesoriaRepositoryImpl implements AsesoriaRepository, PanacheRepositoryBase<Asesoria, UUID> {

    @Override
    public Asesoria save(Asesoria asesoria) {
        if (asesoria.getId() == null) {
            persist(asesoria);
            return asesoria;
        }
        return getEntityManager().merge(asesoria);
    }

    @Override
    public boolean existeAsesorParaTesis(UUID tesisId) {
        if (tesisId == null) {
            return false;
        }
        return count("tesisId = ?1 and upper(tipo) = 'ASESOR' and active = true", tesisId) > 0;
    }

    @Override
    public java.util.Optional<unmsm.edu.pe.tesis.domain.entities.Asesoria> buscarPorTesisYTipo(UUID tesisId, String tipo) {
        if (tesisId == null || tipo == null) {
            return java.util.Optional.empty();
        }
        return find("tesisId = ?1 and upper(tipo) = ?2 and active = true", tesisId, tipo.toUpperCase())
                .firstResultOptional();
    }

    @Override
    public long contarAsesoriasDeDocente(UUID docenteId) {
        return count("docenteId = ?1 and upper(tipo) = 'ASESOR' and active = true", docenteId);
    }
}
