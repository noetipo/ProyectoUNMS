package unmsm.edu.pe.configuracion.domain.services;

import unmsm.edu.pe.configuracion.application.dto.ParametroSistemaRequest;
import unmsm.edu.pe.configuracion.application.dto.ParametroSistemaResponse;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;

public interface ParametroSistemaService {
    PageResponse<ParametroSistemaResponse> listar(String search, int page, int size);
    ParametroSistemaResponse obtener(UUID id);
    ParametroSistemaResponse crear(ParametroSistemaRequest request);
    ParametroSistemaResponse actualizar(UUID id, ParametroSistemaRequest request);
    void eliminar(UUID id);
}
