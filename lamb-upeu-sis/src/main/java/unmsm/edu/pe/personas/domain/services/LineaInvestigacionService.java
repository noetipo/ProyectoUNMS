package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.LineaInvestigacionRequest;
import unmsm.edu.pe.personas.application.dto.LineaInvestigacionResponse;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;

public interface LineaInvestigacionService {
    PageResponse<LineaInvestigacionResponse> listar(String search, int page, int size);
    LineaInvestigacionResponse obtener(UUID id);
    LineaInvestigacionResponse crear(LineaInvestigacionRequest request);
    LineaInvestigacionResponse actualizar(UUID id, LineaInvestigacionRequest request);
    void eliminar(UUID id);
}
