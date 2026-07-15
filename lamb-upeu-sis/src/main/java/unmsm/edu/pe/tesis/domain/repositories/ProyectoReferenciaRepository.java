package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoReferencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoReferenciaRepository {
    ProyectoReferencia save(ProyectoReferencia referencia);
    Optional<ProyectoReferencia> buscarPorId(UUID id);
    List<ProyectoReferencia> listarPorProyecto(UUID proyectoId);
    void eliminar(ProyectoReferencia referencia);
}
