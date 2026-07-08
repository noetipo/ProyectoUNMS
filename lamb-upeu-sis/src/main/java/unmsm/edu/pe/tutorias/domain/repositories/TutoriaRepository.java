package unmsm.edu.pe.tutorias.domain.repositories;

import unmsm.edu.pe.tutorias.domain.entities.Tutoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TutoriaRepository {
    Tutoria save(Tutoria tutoria);
    Optional<Tutoria> buscarPorId(UUID id);

    /** Tutoría vigente (actual = true) de un estudiante, si existe. */
    Optional<Tutoria> findVigenteByEstudiante(UUID estudianteId);

    /** Nº de estudiantes con tutoría vigente de un docente (para el cupo). */
    long countActualesByDocente(UUID docenteId);

    /** Cierra (actual=false, fecha_fin) la tutoría vigente del estudiante. Bulk update. */
    int cerrarVigentePorEstudiante(UUID estudianteId, java.time.LocalDate fechaFin);

    /** Historial completo del estudiante (vigente primero, luego por fecha desc). */
    List<Tutoria> findByEstudianteId(UUID estudianteId);

    /**
     * Docentes activos (cualquiera puede ser tutor). Cada fila: [persona_id,
     * apellido_paterno, apellido_materno, nombres, codigo_sistema, grado_academico,
     * cupo_maximo_tutoria, estudiantes_actuales].
     */
    List<Object[]> listarTutores(String buscar, int page, int size);
    long contarTutores(String buscar);

    /**
     * Estudiantes filtrados por programa + estado de tutor + texto. Cada fila:
     * [estudiante_id, nombres, apellido_paterno, apellido_materno, codigo_sistema,
     * programa_id, programa_nombre, tutor_id, tutor_nombres, tutor_apellido_paterno,
     * tutor_apellido_materno].
     */
    List<Object[]> listarEstudiantesAsignables(UUID facultadId, UUID programaId, Boolean conTutor, String buscar, int page, int size);
    long contarEstudiantesAsignables(UUID facultadId, UUID programaId, Boolean conTutor, String buscar);
}
