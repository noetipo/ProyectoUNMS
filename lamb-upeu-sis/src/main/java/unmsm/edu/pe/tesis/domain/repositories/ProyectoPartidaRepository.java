package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoPartida;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoPartidaRepository {
    ProyectoPartida save(ProyectoPartida partida);
    Optional<ProyectoPartida> buscarPorId(UUID id);
    List<ProyectoPartida> listarPorProyecto(UUID proyectoId);
    void eliminar(ProyectoPartida partida);
}
