package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.personas.application.dto.EstudianteListItem;
import unmsm.edu.pe.personas.application.dto.EstudiantesResumen;
import unmsm.edu.pe.personas.application.mapper.PersonaMapper;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.services.EstudianteReportService;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class EstudianteReportServiceImpl implements EstudianteReportService {

    @Inject EstudianteRepository estudianteRepository;
    @Inject PersonaMapper mapper;

    @Override
    public PageResponse<EstudianteListItem> listar(String search, UUID facultadId, UUID programaId, String condicion, String nivel, int page, int size) {
        List<EstudianteListItem> content = estudianteRepository
                .listar(search, facultadId, programaId, condicion, nivel, page, size).stream()
                .map(mapper::toEstudianteListItem)
                .collect(Collectors.toList());
        long total = estudianteRepository.contar(search, facultadId, programaId, condicion, nivel);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    public EstudiantesResumen resumen() {
        return EstudiantesResumen.builder()
                .total(estudianteRepository.contarActivos())
                .regulares(estudianteRepository.contarPorCondicion("REGULAR"))
                .egresados(estudianteRepository.contarPorCondicion("EGRESADO"))
                // "con tesis activa" se calculará cuando exista el módulo de tesis
                .conTesisActiva(0)
                .build();
    }
}
