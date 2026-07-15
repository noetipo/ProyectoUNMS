package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoReferencia;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoReferenciaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProyectoReferenciaRepositoryImpl
        implements ProyectoReferenciaRepository, PanacheRepositoryBase<ProyectoReferencia, UUID> {

    @Override
    public ProyectoReferencia save(ProyectoReferencia referencia) {
        if (referencia.getId() == null) {
            persist(referencia);
            return referencia;
        }
        return getEntityManager().merge(referencia);
    }

    @Override
    public Optional<ProyectoReferencia> buscarPorId(UUID id) {
        return find("id = ?1 and active = true", id).firstResultOptional();
    }

    @Override
    public List<ProyectoReferencia> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("orden"), proyectoId);
    }

    @Override
    public void eliminar(ProyectoReferencia referencia) {
        referencia.setActive(false);
        getEntityManager().merge(referencia);
    }
}
