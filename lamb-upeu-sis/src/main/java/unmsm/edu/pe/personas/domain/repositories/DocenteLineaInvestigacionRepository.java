package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.DocenteLineaInvestigacion;

import java.util.List;
import java.util.UUID;

public interface DocenteLineaInvestigacionRepository {
    List<DocenteLineaInvestigacion> findByDocenteId(UUID docenteId);
    List<DocenteLineaInvestigacion> saveAll(List<DocenteLineaInvestigacion> lineas);
    void deleteByDocenteId(UUID docenteId);

    /** ¿El docente tiene asignada esa línea de investigación (activa)? */
    boolean existsByDocenteAndLinea(UUID docenteId, UUID lineaId);

    /**
     * Docentes activos que tienen la línea indicada, para el combo del formulario.
     * Cada fila: [persona_id, apellido_paterno, apellido_materno, nombres,
     * codigo_sistema, grado_academico].
     */
    List<Object[]> listarDocentesPorLinea(UUID lineaId);
}
