package unmsm.edu.pe.tutorias.domain.repositories;

import java.util.List;
import java.util.UUID;

/**
 * Consultas agregadas para el reporte de tutores (solo tutoría vigente).
 * Todas usan GROUP BY / conteos para evitar N+1 y aprovechan idx_tutorias_docente_actual.
 */
public interface ReporteTutoresRepository {

    /** Nº de tutores (docentes con ≥1 tutoría vigente) que cumplen el filtro. */
    long contarTutores(String buscar, UUID facultadId, UUID programaId);

    /** Nº de estudiantes con tutoría vigente (filtro por programa). */
    long contarEstudiantesConTutor(UUID facultadId, UUID programaId);

    /** Nº total de estudiantes activos (filtro por programa) — para 'sin tutor'. */
    long contarEstudiantesTotales(UUID facultadId, UUID programaId);

    /**
     * Tutores paginados. Fila: [persona_id, apellido_paterno, apellido_materno, nombres,
     * grado_academico, cupo_maximo_tutoria, estudiantes, programas_distintos].
     */
    List<Object[]> listarTutores(String buscar, UUID facultadId, UUID programaId, int page, int size);

    /**
     * Estudiantes vigentes de un tutor (lazy). Fila: [persona_id, apellido_paterno,
     * apellido_materno, nombres, codigo_sistema, cod_matricula, programa_nombre,
     * fecha_inicio, anio_ingreso].
     */
    List<Object[]> estudiantesDeTutor(UUID docenteId, int page, int size);
    long contarEstudiantesDeTutor(UUID docenteId);

    /** Igual que estudiantesDeTutor pero con búsqueda por nombre/código (panel del tutor). */
    List<Object[]> tutorandos(UUID docenteId, String buscar, int page, int size);
    long contarTutorandos(UUID docenteId, String buscar);

    /**
     * Estudiantes sin tutoría vigente. Fila: [persona_id, apellido_paterno,
     * apellido_materno, nombres, codigo_sistema, cod_matricula, programa_nombre, anio_ingreso].
     */
    List<Object[]> estudiantesSinTutor(UUID facultadId, UUID programaId, String buscar, int page, int size);
    long contarEstudiantesSinTutor(UUID facultadId, UUID programaId, String buscar);

    /**
     * Datos planos para exportación (todos los del filtro, sin paginar), una fila por
     * (tutor, estudiante), ordenados por tutor. Fila: [tutor_id, t_ap, t_am, t_nombres,
     * grado, cupo, e_ap, e_am, e_nombres, codigo_sistema, cod_matricula, programa_nombre, fecha_inicio].
     */
    List<Object[]> datosExport(String buscar, UUID facultadId, UUID programaId);
}
