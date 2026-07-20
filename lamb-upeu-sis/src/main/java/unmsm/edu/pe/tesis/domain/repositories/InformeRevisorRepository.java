package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.InformeRevisor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InformeRevisorRepository {
    InformeRevisor save(InformeRevisor revisor);
    Optional<InformeRevisor> buscarPorId(UUID id);
    List<InformeRevisor> listarPorProyecto(UUID proyectoId);
    long contarPorProyecto(UUID proyectoId);
    Optional<InformeRevisor> buscarPorProyectoYDocente(UUID proyectoId, UUID docenteId);

    /** Bandeja del jurado informante:
     * cols [tesis_id, proyecto_id, revisor_id, estado, presidente, apPat, apMat, nombres, codigo, programa, titulo]. */
    List<Object[]> bandejaDeJurado(UUID docenteId);
}
