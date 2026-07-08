package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.application.dto.CentroLaboralRequest;
import unmsm.edu.pe.personas.application.dto.CentroLaboralResponse;
import unmsm.edu.pe.personas.domain.entities.CentroLaboral;
import unmsm.edu.pe.personas.domain.repositories.CentroLaboralRepository;
import unmsm.edu.pe.personas.domain.services.CentroLaboralService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CentroLaboralServiceImpl implements CentroLaboralService {

    @Inject CentroLaboralRepository centroRepository;

    @Override
    public PageResponse<CentroLaboralResponse> listar(String search, int page, int size) {
        var content = centroRepository.listar(search, page, size).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return PageResponse.of(content, centroRepository.contar(search), page, size);
    }

    @Override
    public CentroLaboralResponse obtener(UUID id) {
        return toResponse(centroRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Centro laboral no encontrado: " + id)));
    }

    @Override
    @Transactional
    public CentroLaboralResponse crear(CentroLaboralRequest request) {
        if (centroRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe un centro laboral con el nombre: " + request.getNombre());
        }
        CentroLaboral centro = CentroLaboral.builder()
                .codigoSistema(String.format("CEN-%05d", centroRepository.contarTotal() + 1))
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
        return toResponse(centroRepository.save(centro));
    }

    @Override
    @Transactional
    public CentroLaboralResponse actualizar(UUID id, CentroLaboralRequest request) {
        CentroLaboral centro = centroRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Centro laboral no encontrado: " + id));
        if (request.getNombre() != null && !request.getNombre().equalsIgnoreCase(centro.getNombre())
                && centroRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe un centro laboral con el nombre: " + request.getNombre());
        }
        if (request.getNombre() != null) centro.setNombre(request.getNombre());
        centro.setDescripcion(request.getDescripcion());
        return toResponse(centroRepository.save(centro));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        CentroLaboral centro = centroRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Centro laboral no encontrado: " + id));
        centro.setActive(false);
        centroRepository.save(centro);
    }

    private CentroLaboralResponse toResponse(CentroLaboral c) {
        return CentroLaboralResponse.builder()
                .id(c.getId()).codigoSistema(c.getCodigoSistema())
                .nombre(c.getNombre()).descripcion(c.getDescripcion())
                .activo(c.getActive()).build();
    }
}
