package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.panache.common.Sort;
import unmsm.edu.pe.tesis.domain.entities.ProyectoObjetivo;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoObjetivoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProyectoObjetivoRepositoryImpl
        implements ProyectoObjetivoRepository, PanacheRepositoryBase<ProyectoObjetivo, UUID> {

    @Override
    public ProyectoObjetivo save(ProyectoObjetivo objetivo) {
        if (objetivo.getId() == null) {
            persist(objetivo);
            return objetivo;
        }
        return getEntityManager().merge(objetivo);
    }

    @Override
    public Optional<ProyectoObjetivo> buscarPorId(UUID id) {
        return find("id = ?1 and active = true", id).firstResultOptional();
    }

    @Override
    public List<ProyectoObjetivo> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("orden"), proyectoId);
    }

    @Override
    public void eliminar(ProyectoObjetivo objetivo) {
        objetivo.setActive(false);
        getEntityManager().merge(objetivo);
    }
}
