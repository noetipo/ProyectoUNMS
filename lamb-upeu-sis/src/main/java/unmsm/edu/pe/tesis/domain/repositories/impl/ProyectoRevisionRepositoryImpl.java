package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevision;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProyectoRevisionRepositoryImpl
        implements ProyectoRevisionRepository, PanacheRepositoryBase<ProyectoRevision, UUID> {

    @Override
    public ProyectoRevision save(ProyectoRevision revision) {
        if (revision.getId() == null) {
            persist(revision);
            return revision;
        }
        return getEntityManager().merge(revision);
    }

    @Override
    public Optional<ProyectoRevision> buscarPorId(UUID id) {
        return find("id = ?1 and active = true", id).firstResultOptional();
    }

    @Override
    public Optional<ProyectoRevision> buscarPorProyectoYCampo(UUID proyectoId, String campo) {
        return find("proyectoId = ?1 and campo = ?2 and active = true", proyectoId, campo).firstResultOptional();
    }

    @Override
    public List<ProyectoRevision> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", proyectoId);
    }

    @Override
    public List<UUID> idsPorProyecto(UUID proyectoId) {
        return getEntityManager()
                .createQuery("select r.id from ProyectoRevision r where r.proyectoId = :p", UUID.class)
                .setParameter("p", proyectoId)
                .getResultList();
    }

    @Override
    public void eliminarPorProyecto(UUID proyectoId) {
        delete("proyectoId = ?1", proyectoId);
    }
}
