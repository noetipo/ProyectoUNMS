package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.CrearSolicitudRequest;
import unmsm.edu.pe.tesis.application.dto.ResponderSolicitudRequest;
import unmsm.edu.pe.tesis.application.dto.SolicitudBandejaItem;
import unmsm.edu.pe.tesis.application.dto.SolicitudResponse;

import java.util.UUID;

public interface SolicitudAsesoriaService {

    /** Estudiante autenticado registra una carta de solicitud a un docente. */
    SolicitudResponse crear(CrearSolicitudRequest request);

    /** Bandeja del docente autenticado (por estado, default PENDIENTE). */
    PageResponse<SolicitudBandejaItem> bandeja(String estado, int page, int size);

    /** El docente autenticado acepta o rechaza una solicitud PENDIENTE suya. */
    SolicitudResponse responder(UUID id, ResponderSolicitudRequest request);

    /** El estudiante autenticado cancela una solicitud PENDIENTE suya. */
    SolicitudResponse cancelar(UUID id);
}
