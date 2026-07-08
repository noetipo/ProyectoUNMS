package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.CargoRequest;
import unmsm.edu.pe.personas.application.dto.CargoResponse;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;

public interface CargoService {
    PageResponse<CargoResponse> listar(String search, int page, int size);
    CargoResponse obtener(UUID id);
    CargoResponse crear(CargoRequest request);
    CargoResponse actualizar(UUID id, CargoRequest request);
    void eliminar(UUID id);
}
