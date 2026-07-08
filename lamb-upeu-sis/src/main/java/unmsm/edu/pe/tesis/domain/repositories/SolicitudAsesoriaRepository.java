package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudAsesoriaRepository {
    SolicitudAsesoria save(SolicitudAsesoria solicitud);
    Optional<SolicitudAsesoria> buscarPorId(UUID id);

    /** ¿El estudiante ya tiene una solicitud PENDIENTE? (regla de negocio) */
    boolean existePendientePorEstudiante(UUID estudianteId);

    /** Última solicitud del estudiante (cualquier estado), la más reciente por fecha. */
    Optional<SolicitudAsesoria> ultimaDeEstudiante(UUID estudianteId);

    /**
     * Bandeja del docente. Cada fila: [id, estudiante_persona_id, nombres,
     * apellido_paterno, apellido_materno, codigo_sistema, programa_nombre,
     * linea_nombre, titulo_tentativo, tipo, estado, fecha_solicitud,
     * fecha_respuesta, motivo_respuesta, mensaje].
     */
    List<Object[]> listarPorDocente(UUID docenteId, EstadoSolicitud estado, int page, int size);
    long contarPorDocente(UUID docenteId, EstadoSolicitud estado);

    /**
     * Solicitudes del estudiante. Cada fila: [id, docente_persona_id, nombres,
     * apellido_paterno, apellido_materno, codigo_sistema, grado_academico,
     * linea_nombre, titulo_tentativo, tipo, estado, fecha_solicitud,
     * fecha_respuesta, motivo_respuesta, mensaje].
     */
    List<Object[]> listarPorEstudiante(UUID estudianteId, EstadoSolicitud estado, int page, int size);
    long contarPorEstudiante(UUID estudianteId, EstadoSolicitud estado);
}
