package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisionEvento;

import java.util.List;
import java.util.UUID;

public interface ProyectoRevisionEventoRepository {
    ProyectoRevisionEvento save(ProyectoRevisionEvento evento);
    List<ProyectoRevisionEvento> listarPorRevision(UUID revisionId);
    List<ProyectoRevisionEvento> listarPorRevisiones(List<UUID> revisionIds);
    /** Elimina físicamente todos los eventos de las revisiones dadas. */
    void eliminarPorRevisiones(List<UUID> revisionIds);
}
