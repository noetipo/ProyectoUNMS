package unmsm.edu.pe.configuracion.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.configuracion.application.dto.ParametroSistemaRequest;
import unmsm.edu.pe.configuracion.application.dto.ParametroSistemaResponse;
import unmsm.edu.pe.configuracion.domain.entities.ParametroSistema;
import unmsm.edu.pe.configuracion.domain.repositories.ParametroSistemaRepository;
import unmsm.edu.pe.configuracion.domain.services.ParametroSistemaService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ParametroSistemaServiceImpl implements ParametroSistemaService {

    @Inject ParametroSistemaRepository parametroRepository;

    @Override
    public PageResponse<ParametroSistemaResponse> listar(String search, int page, int size) {
        var content = parametroRepository.listar(search, page, size).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return PageResponse.of(content, parametroRepository.contar(search), page, size);
    }

    @Override
    public ParametroSistemaResponse obtener(UUID id) {
        return toResponse(parametroRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Parámetro no encontrado: " + id)));
    }

    @Override
    @Transactional
    public ParametroSistemaResponse crear(ParametroSistemaRequest request) {
        if (parametroRepository.existsByClave(request.getClave())) {
            throw new BusinessException("Ya existe un parámetro con la clave: " + request.getClave());
        }
        ParametroSistema parametro = ParametroSistema.builder()
                .clave(request.getClave())
                .valor(request.getValor())
                .descripcion(trimToNull(request.getDescripcion()))
                .build();
        return toResponse(parametroRepository.save(parametro));
    }

    @Override
    @Transactional
    public ParametroSistemaResponse actualizar(UUID id, ParametroSistemaRequest request) {
        ParametroSistema parametro = parametroRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Parámetro no encontrado: " + id));
        // La clave es INMUTABLE tras crear: solo se actualizan valor y descripción.
        parametro.setValor(request.getValor());
        parametro.setDescripcion(trimToNull(request.getDescripcion()));
        return toResponse(parametroRepository.save(parametro));
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        ParametroSistema parametro = parametroRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Parámetro no encontrado: " + id));
        parametro.setActive(false);
        parametroRepository.save(parametro);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private ParametroSistemaResponse toResponse(ParametroSistema p) {
        return ParametroSistemaResponse.builder()
                .id(p.getId()).clave(p.getClave()).valor(p.getValor())
                .descripcion(p.getDescripcion()).activo(p.getActive()).build();
    }
}
