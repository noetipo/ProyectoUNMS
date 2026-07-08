package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.personas.application.dto.DocenteListItem;
import unmsm.edu.pe.personas.application.dto.DocentesResumen;
import unmsm.edu.pe.personas.application.mapper.PersonaMapper;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.services.DocenteReportService;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class DocenteReportServiceImpl implements DocenteReportService {

    @Inject DocenteRepository docenteRepository;
    @Inject PersonaMapper mapper;

    @Override
    public PageResponse<DocenteListItem> listar(String search, String grado, String categoria, String condicion, int page, int size) {
        List<DocenteListItem> content = docenteRepository
                .listar(search, grado, categoria, condicion, page, size).stream()
                .map(mapper::toDocenteListItem)
                .collect(Collectors.toList());
        long total = docenteRepository.contar(search, grado, categoria, condicion);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    public DocentesResumen resumen() {
        return DocentesResumen.builder()
                .total(docenteRepository.contarActivos())
                .doctores(docenteRepository.contarPorGrado("DOCTOR"))
                .asesorando(docenteRepository.contarAsesorando())
                .enJurados(docenteRepository.contarEnJurados())
                .build();
    }
}
