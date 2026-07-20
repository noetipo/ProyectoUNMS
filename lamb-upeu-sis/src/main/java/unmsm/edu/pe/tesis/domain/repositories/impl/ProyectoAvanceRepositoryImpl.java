package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoAvance;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoAvanceRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProyectoAvanceRepositoryImpl
        implements ProyectoAvanceRepository, PanacheRepositoryBase<ProyectoAvance, UUID> {

    @Override
    public ProyectoAvance save(ProyectoAvance avance) {
        if (avance.getId() == null) {
            persist(avance);
            return avance;
        }
        return getEntityManager().merge(avance);
    }

    @Override
    public List<ProyectoAvance> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("fechaEvaluacion").descending(), proyectoId);
    }
}
