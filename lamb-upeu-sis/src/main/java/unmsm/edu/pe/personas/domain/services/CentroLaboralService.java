package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.CentroLaboralRequest;
import unmsm.edu.pe.personas.application.dto.CentroLaboralResponse;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;

public interface CentroLaboralService {
    PageResponse<CentroLaboralResponse> listar(String search, int page, int size);
    CentroLaboralResponse obtener(UUID id);
    CentroLaboralResponse crear(CentroLaboralRequest request);
    CentroLaboralResponse actualizar(UUID id, CentroLaboralRequest request);
    void eliminar(UUID id);
}
