package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.ProyectoPartida;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoPartidaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProyectoPartidaRepositoryImpl
        implements ProyectoPartidaRepository, PanacheRepositoryBase<ProyectoPartida, UUID> {

    @Override
    public ProyectoPartida save(ProyectoPartida partida) {
        if (partida.getId() == null) {
            persist(partida);
            return partida;
        }
        return getEntityManager().merge(partida);
    }

    @Override
    public Optional<ProyectoPartida> buscarPorId(UUID id) {
        return find("id = ?1 and active = true", id).firstResultOptional();
    }

    @Override
    public List<ProyectoPartida> listarPorProyecto(UUID proyectoId) {
        return list("proyectoId = ?1 and active = true", Sort.by("orden"), proyectoId);
    }

    @Override
    public void eliminar(ProyectoPartida partida) {
        partida.setActive(false);
        getEntityManager().merge(partida);
    }
}
