package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisionEvento;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisionEventoRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProyectoRevisionEventoRepositoryImpl
        implements ProyectoRevisionEventoRepository, PanacheRepositoryBase<ProyectoRevisionEvento, UUID> {

    @Override
    public ProyectoRevisionEvento save(ProyectoRevisionEvento evento) {
        if (evento.getId() == null) {
            persist(evento);
            return evento;
        }
        return getEntityManager().merge(evento);
    }

    @Override
    public List<ProyectoRevisionEvento> listarPorRevision(UUID revisionId) {
        return list("revisionId = ?1 and active = true", Sort.by("fechaEvento"), revisionId);
    }

    @Override
    public List<ProyectoRevisionEvento> listarPorRevisiones(List<UUID> revisionIds) {
        if (revisionIds == null || revisionIds.isEmpty()) {
            return List.of();
        }
        return list("revisionId in ?1 and active = true", Sort.by("fechaEvento"), revisionIds);
    }

    @Override
    public void eliminarPorRevisiones(List<UUID> revisionIds) {
        if (revisionIds == null || revisionIds.isEmpty()) {
            return;
        }
        delete("revisionId in ?1", revisionIds);
    }
}
