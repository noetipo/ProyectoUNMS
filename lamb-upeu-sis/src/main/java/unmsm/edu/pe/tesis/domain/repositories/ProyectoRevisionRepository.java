package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoRevision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoRevisionRepository {
    ProyectoRevision save(ProyectoRevision revision);
    Optional<ProyectoRevision> buscarPorId(UUID id);
    Optional<ProyectoRevision> buscarPorProyectoYCampo(UUID proyectoId, String campo);
    List<ProyectoRevision> listarPorProyecto(UUID proyectoId);

    /** Ids de todas las revisiones del proyecto (activas o no), para borrado en cascada. */
    List<UUID> idsPorProyecto(UUID proyectoId);
    /** Elimina físicamente todas las revisiones del proyecto (activas o no). */
    void eliminarPorProyecto(UUID proyectoId);
}
