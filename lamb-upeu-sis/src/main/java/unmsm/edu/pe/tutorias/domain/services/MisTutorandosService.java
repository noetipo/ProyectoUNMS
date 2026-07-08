package unmsm.edu.pe.tutorias.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tutorias.application.dto.MisTutorandosResumen;
import unmsm.edu.pe.tutorias.application.dto.TutorEstudianteItem;

public interface MisTutorandosService {
    /** Resumen del tutor autenticado (nombre, total de tutorandos vigentes, cupo). */
    MisTutorandosResumen resumen();

    /** Tutorandos vigentes del tutor autenticado (búsqueda + paginación). */
    PageResponse<TutorEstudianteItem> tutorandos(String buscar, int page, int size);
}
