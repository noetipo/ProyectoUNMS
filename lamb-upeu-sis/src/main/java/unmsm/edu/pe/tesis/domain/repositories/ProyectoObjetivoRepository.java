package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoObjetivo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoObjetivoRepository {
    ProyectoObjetivo save(ProyectoObjetivo objetivo);
    Optional<ProyectoObjetivo> buscarPorId(UUID id);
    List<ProyectoObjetivo> listarPorProyecto(UUID proyectoId);
    void eliminar(ProyectoObjetivo objetivo);
}
