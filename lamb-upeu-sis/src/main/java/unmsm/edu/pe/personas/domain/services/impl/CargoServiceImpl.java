package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.application.dto.CargoRequest;
import unmsm.edu.pe.personas.application.dto.CargoResponse;
import unmsm.edu.pe.personas.domain.entities.Cargo;
import unmsm.edu.pe.personas.domain.repositories.CargoRepository;
import unmsm.edu.pe.personas.domain.services.CargoService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CargoServiceImpl implements CargoService {

    @Inject CargoRepository cargoRepository;

    @Override
    public PageResponse<CargoResponse> listar(String search, int page, int size) {
        var content = cargoRepository.listar(search, page, size).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return PageResponse.of(content, cargoRepository.contar(search), page, size);
    }

    @Override
    public CargoResponse obtener(UUID id) {
        return toResponse(cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Cargo no encontrado: " + id)));
    }

    @Override
    @Transactional
    public CargoResponse crear(CargoRequest request) {
        if (cargoRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe un cargo con el nombre: " + request.getNombre());
        }
        Cargo cargo = Cargo.builder()
                .codigoSistema(String.format("CAR-%05d", cargoRepository.contarTotal() + 1))
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
        return toResponse(cargoRepository.save(cargo));
    }

    @Override
    @Transactional
    public CargoResponse actualizar(UUID id, CargoRequest request) {
        Cargo cargo = cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Cargo no encontrado: " + id));
        if (request.getNombre() != null && !request.getNombre().equalsIgnoreCase(cargo.getNombre())
                && cargoRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe un cargo con el nombre: " + request.getNombre());
        }
        if (request.getNombre() != null) cargo.setNombre(request.getNombre());
        cargo.setDescripcion(request.getDescripcion());
        return toResponse(cargoRepository.save(cargo));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        Cargo cargo = cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Cargo no encontrado: " + id));
        cargo.setActive(false);
        cargoRepository.save(cargo);
    }

    private CargoResponse toResponse(Cargo c) {
        return CargoResponse.builder()
                .id(c.getId()).codigoSistema(c.getCodigoSistema())
                .nombre(c.getNombre()).descripcion(c.getDescripcion())
                .activo(c.getActive()).build();
    }
}
