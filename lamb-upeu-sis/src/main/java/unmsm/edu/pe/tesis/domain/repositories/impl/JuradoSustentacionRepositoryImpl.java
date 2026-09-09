package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.JuradoSustentacion;
import unmsm.edu.pe.tesis.domain.repositories.JuradoSustentacionRepository;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JuradoSustentacionRepositoryImpl
        implements JuradoSustentacionRepository, PanacheRepositoryBase<JuradoSustentacion, UUID> {

    @Override
    public JuradoSustentacion save(JuradoSustentacion jurado) {
        if (jurado.getId() == null) {
            persist(jurado);
            return jurado;
        }
        return getEntityManager().merge(jurado);
    }

    @Override
    public List<JuradoSustentacion> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("orden"), proyectoId);
    }

    @Override
    public long contarPorProyecto(UUID proyectoId) {
        return count("proyectoId = ?1 and active = true", proyectoId);
    }
}
