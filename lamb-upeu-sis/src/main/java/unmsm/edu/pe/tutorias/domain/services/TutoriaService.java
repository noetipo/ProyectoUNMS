package unmsm.edu.pe.tutorias.domain.services;

import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tutorias.application.dto.AsignarEnBloqueRequest;
import unmsm.edu.pe.tutorias.application.dto.AsignarEnBloqueResponse;
import unmsm.edu.pe.tutorias.application.dto.EstudianteAsignableItem;
import unmsm.edu.pe.tutorias.application.dto.TutorComboItem;
import unmsm.edu.pe.tutorias.application.dto.TutorVigenteItem;
import unmsm.edu.pe.tutorias.application.dto.TutoriaHistorialItem;

import java.util.List;
import java.util.UUID;

public interface TutoriaService {

    PageResponse<TutorComboItem> buscarTutores(String buscar, int page, int size);

    PageResponse<EstudianteAsignableItem> estudiantesAsignables(UUID facultadId, UUID programaId, Boolean conTutor, String buscar, int page, int size);

    /** Tutor vigente del estudiante (o null). */
    TutorVigenteItem tutorVigente(UUID estudianteId);

    List<TutoriaHistorialItem> historial(UUID estudianteId);

    /**
     * Núcleo reutilizable: cierra la tutoría vigente (si es de otro docente) y abre
     * una nueva; idempotente si ya es el mismo tutor; valida el cupo del docente.
     * Debe invocarse dentro de una transacción (p. ej. desde el guardado de estudiante).
     */
    void asignar(Estudiante estudiante, Docente docente, String motivoCambio);

    /** Asignación individual (resuelve entidades y aplica {@link #asignar}). */
    void asignarIndividual(UUID estudianteId, UUID tutorId, String motivoCambio);

    /** Asignación en bloque: valida cupo total y aplica cierre+apertura por estudiante. */
    AsignarEnBloqueResponse asignarEnBloque(AsignarEnBloqueRequest request);

    /**
     * Finaliza la tutoría vigente del estudiante con ese tutor (queda sin tutor,
     * conservando el historial). Valida que la vigente corresponda al tutor.
     */
    void finalizar(UUID docenteId, UUID estudianteId, String motivo);
}
