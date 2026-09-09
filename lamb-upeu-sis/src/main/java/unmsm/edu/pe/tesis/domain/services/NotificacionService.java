package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.tesis.application.dto.NotificacionItem;

import java.util.List;

/** Notificaciones accionables del usuario autenticado, según su rol. */
public interface NotificacionService {
    List<NotificacionItem> paraUsuarioActual();

    /**
     * Notificaciones del estudiante dueño de {@code persona}, sin depender del usuario autenticado.
     * La usa el scheduler de correos para recorrer a todos los estudiantes con tesis activa.
     */
    List<NotificacionItem> notificacionesDeEstudiante(Persona persona);

    /** Igual que {@link #notificacionesDeEstudiante}, pero del docente en su rol de asesor. */
    List<NotificacionItem> notificacionesDeAsesor(Persona persona);

    /** Igual que {@link #notificacionesDeEstudiante}, pero del docente en su rol de revisor. */
    List<NotificacionItem> notificacionesDeRevisor(Persona persona);
}
