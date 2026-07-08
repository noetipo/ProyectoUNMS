package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import unmsm.edu.pe.tesis.domain.entities.TesisAutor;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;

import java.util.UUID;

@ApplicationScoped
public class TesisAutorRepositoryImpl implements TesisAutorRepository, PanacheRepositoryBase<TesisAutor, UUID> {

    @Override
    public TesisAutor save(TesisAutor autor) {
        if (autor.getId() == null) {
            persist(autor);
            return autor;
        }
        return getEntityManager().merge(autor);
    }

    @Override
    public UUID tesisActivaId(UUID estudianteId) {
        try {
            return getEntityManager().createQuery(
                            "select ta.tesisId from TesisAutor ta "
                                    + "where ta.estudianteId = :est and ta.esActiva = true and ta.active = true", UUID.class)
                    .setParameter("est", estudianteId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public UUID estudianteDeTesis(UUID tesisId) {
        try {
            return getEntityManager().createQuery(
                            "select ta.estudianteId from TesisAutor ta "
                                    + "where ta.tesisId = :t and ta.esActiva = true and ta.active = true", UUID.class)
                    .setParameter("t", tesisId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
