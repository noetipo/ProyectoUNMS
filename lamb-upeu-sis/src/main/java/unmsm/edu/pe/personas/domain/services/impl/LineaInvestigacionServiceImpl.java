package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.application.dto.LineaInvestigacionRequest;
import unmsm.edu.pe.personas.application.dto.LineaInvestigacionResponse;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.services.LineaInvestigacionService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class LineaInvestigacionServiceImpl implements LineaInvestigacionService {

    @Inject LineaInvestigacionRepository lineaInvestigacionRepository;

    @Override
    public PageResponse<LineaInvestigacionResponse> listar(String search, int page, int size) {
        var content = lineaInvestigacionRepository.listar(search, page, size).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return PageResponse.of(content, lineaInvestigacionRepository.contar(search), page, size);
    }

    @Override
    public LineaInvestigacionResponse obtener(UUID id) {
        return toResponse(lineaInvestigacionRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Línea de investigación no encontrada: " + id)));
    }

    @Override
    @Transactional
    public LineaInvestigacionResponse crear(LineaInvestigacionRequest request) {
        if (lineaInvestigacionRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe una línea de investigación con el nombre: " + request.getNombre());
        }
        LineaInvestigacion linea = LineaInvestigacion.builder()
                .codigo(String.format("LIN-%05d", lineaInvestigacionRepository.contarTotal() + 1))
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
        return toResponse(lineaInvestigacionRepository.save(linea));
    }

    @Override
    @Transactional
    public LineaInvestigacionResponse actualizar(UUID id, LineaInvestigacionRequest request) {
        LineaInvestigacion linea = lineaInvestigacionRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Línea de investigación no encontrada: " + id));
        if (request.getNombre() != null && !request.getNombre().equalsIgnoreCase(linea.getNombre())
                && lineaInvestigacionRepository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe una línea de investigación con el nombre: " + request.getNombre());
        }
        if (request.getNombre() != null) linea.setNombre(request.getNombre());
        linea.setDescripcion(request.getDescripcion());
        return toResponse(lineaInvestigacionRepository.save(linea));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        LineaInvestigacion linea = lineaInvestigacionRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Línea de investigación no encontrada: " + id));
        linea.setActive(false);
        lineaInvestigacionRepository.save(linea);
    }

    private LineaInvestigacionResponse toResponse(LineaInvestigacion l) {
        return LineaInvestigacionResponse.builder()
                .id(l.getId()).codigoSistema(l.getCodigo())
                .nombre(l.getNombre()).descripcion(l.getDescripcion())
                .activo(l.getActive()).build();
    }
}
