package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.EstudianteListItem;
import unmsm.edu.pe.personas.application.dto.EstudiantesResumen;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;

public interface EstudianteReportService {
    PageResponse<EstudianteListItem> listar(String search, UUID facultadId, UUID programaId, String condicion, String nivel, int page, int size);
    EstudiantesResumen resumen();
}
