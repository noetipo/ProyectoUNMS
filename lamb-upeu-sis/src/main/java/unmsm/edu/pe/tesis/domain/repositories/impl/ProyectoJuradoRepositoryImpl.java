package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoJurado;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoJuradoRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProyectoJuradoRepositoryImpl
        implements ProyectoJuradoRepository, PanacheRepositoryBase<ProyectoJurado, UUID> {

    @Override
    public ProyectoJurado save(ProyectoJurado jurado) {
        if (jurado.getId() == null) {
            persist(jurado);
            return jurado;
        }
        return getEntityManager().merge(jurado);
    }

    @Override
    public List<ProyectoJurado> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("orden"), proyectoId);
    }

    @Override
    public long contarPorProyecto(UUID proyectoId) {
        return count("proyectoId = ?1 and active = true", proyectoId);
    }
}
