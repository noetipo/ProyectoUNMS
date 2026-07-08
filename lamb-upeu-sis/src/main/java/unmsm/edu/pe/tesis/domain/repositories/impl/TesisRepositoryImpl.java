package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TesisRepositoryImpl implements TesisRepository, PanacheRepositoryBase<Tesis, UUID> {

    @Override
    public Tesis save(Tesis tesis) {
        if (tesis.getId() == null) {
            persist(tesis);
            return tesis;
        }
        return getEntityManager().merge(tesis);
    }

    @Override
    public Optional<Tesis> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public Optional<Tesis> tesisActivaDeEstudiante(UUID estudianteId) {
        try {
            Tesis t = getEntityManager().createQuery(
                            "select t from Tesis t, TesisAutor ta "
                                    + "where ta.tesisId = t.id and ta.estudianteId = :est "
                                    + "and ta.esActiva = true and ta.active = true and t.active = true", Tesis.class)
                    .setParameter("est", estudianteId)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(t);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existeTesisActiva(UUID estudianteId) {
        Query q = getEntityManager().createQuery(
                "select count(ta) from TesisAutor ta "
                        + "where ta.estudianteId = :est and ta.esActiva = true and ta.active = true");
        q.setParameter("est", estudianteId);
        return ((Number) q.getSingleResult()).longValue() > 0;
    }

    @Override
    public boolean tieneAsesor(UUID tesisId) {
        if (tesisId == null) {
            return false;
        }
        Query q = getEntityManager().createQuery(
                "select count(a) from Asesoria a "
                        + "where a.tesisId = :tesisId and upper(a.tipo) = 'ASESOR' and a.active = true");
        q.setParameter("tesisId", tesisId);
        return ((Number) q.getSingleResult()).longValue() > 0;
    }
}
