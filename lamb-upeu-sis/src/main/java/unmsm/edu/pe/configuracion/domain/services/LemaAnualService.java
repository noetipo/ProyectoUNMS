package unmsm.edu.pe.configuracion.domain.services;

import unmsm.edu.pe.configuracion.application.dto.LemaAnualRequest;
import unmsm.edu.pe.configuracion.application.dto.LemaAnualResponse;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;

public interface LemaAnualService {
    PageResponse<LemaAnualResponse> listar(String search, int page, int size);
    LemaAnualResponse obtener(UUID id);
    LemaAnualResponse crear(LemaAnualRequest request);
    LemaAnualResponse actualizar(UUID id, LemaAnualRequest request);
    /** Marca el lema como vigente y desactiva el que estuviera vigente para su año. */
    LemaAnualResponse activar(UUID id);
    /** Quita la vigencia del lema. */
    LemaAnualResponse desactivar(UUID id);
}
