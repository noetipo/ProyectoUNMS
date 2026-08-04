package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoRevisorRepository {
    ProyectoRevisor save(ProyectoRevisor revisor);
    Optional<ProyectoRevisor> buscarPorId(UUID id);
    List<ProyectoRevisor> listarPorProyecto(UUID proyectoId);
    long contarPorProyecto(UUID proyectoId);
    /** Docentes seleccionables como revisor: [persona_id, nombres, apPaterno, apMaterno, categoria]. */
    List<Object[]> docentesOpcion();

    /** Docentes que llevan una línea de investigación dada (candidatos a revisor de esa línea). */
    List<Object[]> docentesOpcionPorLinea(UUID lineaId);

    /** El revisor (registro) de un proyecto para un docente concreto. */
    Optional<ProyectoRevisor> buscarPorProyectoYDocente(UUID proyectoId, UUID docenteId);

    /**
     * Bandeja del revisor: proyectos donde el docente es revisor.
     * Cols: [tesis_id, proyecto_id, revisor_id, estado_revisor, puntaje_total,
     *        apPaterno, apMaterno, nombres, codigo, programa, titulo, fecha_recepcion].
     */
    List<Object[]> bandejaDeRevisor(UUID docenteId);
}
