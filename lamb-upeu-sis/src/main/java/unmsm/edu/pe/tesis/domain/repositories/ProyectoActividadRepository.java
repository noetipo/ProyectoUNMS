package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoActividad;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoActividadRepository {
    ProyectoActividad save(ProyectoActividad actividad);
    Optional<ProyectoActividad> buscarPorId(UUID id);
    List<ProyectoActividad> listarPorProyecto(UUID proyectoId);
    void eliminar(ProyectoActividad actividad);
}
