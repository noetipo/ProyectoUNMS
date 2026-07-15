package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoActividad;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoActividadRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProyectoActividadRepositoryImpl
        implements ProyectoActividadRepository, PanacheRepositoryBase<ProyectoActividad, UUID> {

    @Override
    public ProyectoActividad save(ProyectoActividad actividad) {
        if (actividad.getId() == null) {
            persist(actividad);
            return actividad;
        }
        return getEntityManager().merge(actividad);
    }

    @Override
    public Optional<ProyectoActividad> buscarPorId(UUID id) {
        return find("id = ?1 and active = true", id).firstResultOptional();
    }

    @Override
    public List<ProyectoActividad> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("orden"), proyectoId);
    }

    @Override
    public void eliminar(ProyectoActividad actividad) {
        actividad.setActive(false);
        getEntityManager().merge(actividad);
    }
}
