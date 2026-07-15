package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.NotificacionItem;

import java.util.List;

/** Notificaciones accionables del usuario autenticado, según su rol. */
public interface NotificacionService {
    List<NotificacionItem> paraUsuarioActual();
}
