package unmsm.edu.pe.configuracion.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.configuracion.application.dto.LemaAnualRequest;
import unmsm.edu.pe.configuracion.application.dto.LemaAnualResponse;
import unmsm.edu.pe.configuracion.domain.entities.LemaAnual;
import unmsm.edu.pe.configuracion.domain.repositories.LemaAnualRepository;
import unmsm.edu.pe.configuracion.domain.services.LemaAnualService;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class LemaAnualServiceImpl implements LemaAnualService {

    @Inject LemaAnualRepository lemaRepository;

    @Override
    public PageResponse<LemaAnualResponse> listar(String search, int page, int size) {
        var content = lemaRepository.listar(search, page, size).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return PageResponse.of(content, lemaRepository.contar(search), page, size);
    }

    @Override
    public LemaAnualResponse obtener(UUID id) {
        return toResponse(lemaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Lema no encontrado: " + id)));
    }

    @Override
    @Transactional
    public LemaAnualResponse crear(LemaAnualRequest request) {
        LemaAnual lema = LemaAnual.builder()
                .anio(request.getAnio())
                .texto(request.getTexto())
                .build();
        // Nace vigente solo si el año aún no tiene un lema vigente (un activo por año).
        lema.setActive(!lemaRepository.existeActivoPorAnio(request.getAnio()));
        return toResponse(lemaRepository.save(lema));
    }

    @Override
    @Transactional
    public LemaAnualResponse actualizar(UUID id, LemaAnualRequest request) {
        LemaAnual lema = lemaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Lema no encontrado: " + id));
        lema.setAnio(request.getAnio());
        lema.setTexto(request.getTexto());
        // Si sigue vigente, garantiza que sea el único vigente de su (posible nuevo) año.
        if (Boolean.TRUE.equals(lema.getActive())) {
            desactivarVigenteDelAnio(lema);
            lema.setActive(true);
        }
        return toResponse(lemaRepository.save(lema));
    }

    @Override
    @Transactional
    public LemaAnualResponse activar(UUID id) {
        LemaAnual lema = lemaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Lema no encontrado: " + id));
        desactivarVigenteDelAnio(lema);
        lema.setActive(true);
        return toResponse(lemaRepository.save(lema));
    }

    @Override
    @Transactional
    public LemaAnualResponse desactivar(UUID id) {
        LemaAnual lema = lemaRepository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Lema no encontrado: " + id));
        lema.setActive(false);
        return toResponse(lemaRepository.save(lema));
    }

    /** Desactiva el lema vigente del año de {@code lema} (si es otro) y aplica el cambio. */
    private void desactivarVigenteDelAnio(LemaAnual lema) {
        lemaRepository.buscarActivoPorAnio(lema.getAnio()).ifPresent(vigente -> {
            if (!vigente.getId().equals(lema.getId())) {
                vigente.setActive(false);
                lemaRepository.save(vigente);
                lemaRepository.flushCambios(); // aplica la baja antes de activar el nuevo (índice único parcial)
            }
        });
    }

    private LemaAnualResponse toResponse(LemaAnual l) {
        return LemaAnualResponse.builder()
                .id(l.getId()).anio(l.getAnio()).texto(l.getTexto())
                .activo(l.getActive()).build();
    }
}
