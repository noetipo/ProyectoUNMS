package unmsm.edu.pe.configuracion.domain.services;

import unmsm.edu.pe.configuracion.application.dto.FacultadRequest;
import unmsm.edu.pe.configuracion.application.dto.FacultadResponse;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;

public interface FacultadService {
    PageResponse<FacultadResponse> listar(String search, int page, int size);
    FacultadResponse obtener(UUID id);
    FacultadResponse crear(FacultadRequest request);
    FacultadResponse actualizar(UUID id, FacultadRequest request);
    void eliminar(UUID id);
}
