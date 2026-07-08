package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.DocenteListItem;
import unmsm.edu.pe.personas.application.dto.DocentesResumen;
import unmsm.edu.pe.shared.response.PageResponse;

public interface DocenteReportService {
    PageResponse<DocenteListItem> listar(String search, String grado, String categoria, String condicion, int page, int size);
    DocentesResumen resumen();
}
