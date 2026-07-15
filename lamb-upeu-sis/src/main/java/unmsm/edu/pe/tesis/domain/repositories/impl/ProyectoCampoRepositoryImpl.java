package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoCampo;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoCampoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProyectoCampoRepositoryImpl
        implements ProyectoCampoRepository, PanacheRepositoryBase<ProyectoCampo, UUID> {

    @Override
    public ProyectoCampo save(ProyectoCampo campo) {
        if (campo.getId() == null) {
            persist(campo);
            return campo;
        }
        return getEntityManager().merge(campo);
    }

    @Override
    public Optional<ProyectoCampo> buscarPorProyectoYClave(UUID proyectoId, String clave) {
        return find("proyectoId = ?1 and clave = ?2 and active = true", proyectoId, clave).firstResultOptional();
    }

    @Override
    public List<ProyectoCampo> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", proyectoId);
    }
}
