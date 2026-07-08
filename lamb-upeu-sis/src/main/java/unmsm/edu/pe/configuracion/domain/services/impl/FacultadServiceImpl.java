package unmsm.edu.pe.configuracion.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.configuracion.application.dto.FacultadRequest;
import unmsm.edu.pe.configuracion.application.dto.FacultadResponse;
import unmsm.edu.pe.configuracion.domain.entities.Facultad;
import unmsm.edu.pe.configuracion.domain.repositories.FacultadRepository;
import unmsm.edu.pe.configuracion.domain.services.FacultadService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class FacultadServiceImpl implements FacultadService {

    @Inject FacultadRepository facultadRepository;

    @Override
    public PageResponse<FacultadResponse> listar(String search, int page, int size) {
        var content = facultadRepository.listar(search, page, size).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return PageResponse.of(content, facultadRepository.contar(search), page, size);
    }

    @Override
    public FacultadResponse obtener(UUID id) {
        return toResponse(facultadRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Facultad no encontrada: " + id)));
    }

    @Override
    @Transactional
    public FacultadResponse crear(FacultadRequest request) {
        if (facultadRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe una facultad con el nombre: " + request.getNombre());
        }
        if (request.getCodigo() != null && !request.getCodigo().isBlank()
                && facultadRepository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe una facultad con el código: " + request.getCodigo());
        }
        Facultad facultad = Facultad.builder()
                .codigo(trimToNull(request.getCodigo()))
                .nombre(request.getNombre())
                .build();
        return toResponse(facultadRepository.save(facultad));
    }

    @Override
    @Transactional
    public FacultadResponse actualizar(UUID id, FacultadRequest request) {
        Facultad facultad = facultadRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Facultad no encontrada: " + id));
        if (request.getNombre() != null && !request.getNombre().equalsIgnoreCase(facultad.getNombre())
                && facultadRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe una facultad con el nombre: " + request.getNombre());
        }
        String nuevoCodigo = trimToNull(request.getCodigo());
        if (nuevoCodigo != null && !nuevoCodigo.equalsIgnoreCase(facultad.getCodigo())
                && facultadRepository.existsByCodigo(nuevoCodigo)) {
            throw new BusinessException("Ya existe una facultad con el código: " + nuevoCodigo);
        }
        if (request.getNombre() != null) facultad.setNombre(request.getNombre());
        facultad.setCodigo(nuevoCodigo);
        return toResponse(facultadRepository.save(facultad));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        Facultad facultad = facultadRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Facultad no encontrada: " + id));
        facultad.setActive(false);
        facultadRepository.save(facultad);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private FacultadResponse toResponse(Facultad f) {
        return FacultadResponse.builder()
                .id(f.getId()).codigo(f.getCodigo())
                .nombre(f.getNombre()).activo(f.getActive()).build();
    }
}
