package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.enums.NivelPrograma;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.shared.utils.EstadoDerivado;
import unmsm.edu.pe.tesis.application.dto.EstudianteTemaItem;
import unmsm.edu.pe.tesis.application.dto.EstudiantesTemaResumen;
import unmsm.edu.pe.tesis.application.dto.RegistrarTemaRequest;
import unmsm.edu.pe.tesis.application.dto.TemaResponse;
import unmsm.edu.pe.tesis.application.mapper.CoordinadorTemaMapper;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.entities.TesisAutor;
import unmsm.edu.pe.tesis.domain.enums.EstadoTesis;
import unmsm.edu.pe.tesis.domain.repositories.CoordinadorTemaRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.CoordinadorTemaService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CoordinadorTemaServiceImpl implements CoordinadorTemaService {

    @Inject EstudianteRepository estudianteRepository;
    @Inject LineaInvestigacionRepository lineaInvestigacionRepository;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject CoordinadorTemaRepository reporteRepository;
    @Inject CoordinadorTemaMapper mapper;

    @Override
    public EstudiantesTemaResumen resumen(UUID facultadId, UUID programaId) {
        long total = reporteRepository.contarTotal(facultadId, programaId);
        long conTema = reporteRepository.contarConTema(facultadId, programaId);
        return EstudiantesTemaResumen.builder()
                .total(total)
                .conTema(conTema)
                .sinTema(total - conTema)
                .build();
    }

    @Override
    public PageResponse<EstudianteTemaItem> listar(UUID facultadId, UUID programaId, Boolean conTema, String buscar, int page, int size) {
        List<EstudianteTemaItem> content = reporteRepository.estudiantesTema(facultadId, programaId, conTema, buscar, page, size)
                .stream().map(mapper::toItem).collect(Collectors.toList());
        long total = reporteRepository.contarEstudiantesTema(facultadId, programaId, conTema, buscar);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public TemaResponse registrarTema(UUID estudianteId, RegistrarTemaRequest request) {
        Estudiante estudiante = estudianteRepository.findByPersonaId(estudianteId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + estudianteId));

        if (tesisRepository.existeTesisActiva(estudianteId)) {
            throw new BusinessException("El estudiante ya tiene una tesis activa; use la opción de editar el tema");
        }

        LineaInvestigacion linea = lineaActiva(request);
        NivelPrograma nivel = nivelDelEstudiante(estudiante);

        Tesis tesis = tesisRepository.save(Tesis.builder()
                .titulo(request.getTitulo().trim())
                .resumen(trimToNull(request.getResumen()))
                .lineaInvestigacion(linea)
                .nivel(nivel)
                .estado(EstadoTesis.TEMA_REGISTRADO)
                .fechaRegistro(LocalDate.now())
                .build());

        tesisAutorRepository.save(TesisAutor.builder()
                .tesisId(tesis.getId())
                .estudianteId(estudianteId)
                .esActiva(true)
                .build());

        return toResponse(tesis, estudianteId);
    }

    @Override
    @Transactional
    public TemaResponse editarTema(UUID estudianteId, RegistrarTemaRequest request) {
        estudianteRepository.findByPersonaId(estudianteId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + estudianteId));

        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(estudianteId)
                .orElseThrow(() -> new BusinessException("El estudiante no tiene un tema registrado para editar"));

        LineaInvestigacion linea = lineaActiva(request);
        tesis.setTitulo(request.getTitulo().trim());
        tesis.setResumen(trimToNull(request.getResumen()));
        tesis.setLineaInvestigacion(linea);

        return toResponse(tesisRepository.save(tesis), estudianteId);
    }

    @Override
    public TemaResponse temaDeEstudiante(UUID estudianteId) {
        return tesisRepository.tesisActivaDeEstudiante(estudianteId)
                .map(t -> toResponse(t, estudianteId))
                .orElse(null);
    }

    // ── helpers ──
    private LineaInvestigacion lineaActiva(RegistrarTemaRequest request) {
        return lineaInvestigacionRepository.buscarPorId(request.getLineaInvestigacionId())
                .filter(l -> Boolean.TRUE.equals(l.getActive()))
                .orElseThrow(() -> new BusinessException("Línea de investigación no encontrada o inactiva"));
    }

    private NivelPrograma nivelDelEstudiante(Estudiante estudiante) {
        if (estudiante.getPrograma() == null) {
            throw new BusinessException("El estudiante no tiene un programa de posgrado asignado; no se puede derivar el nivel");
        }
        return estudiante.getPrograma().getNivel();
    }

    private TemaResponse toResponse(Tesis tesis, UUID estudianteId) {
        boolean tieneAsesor = tesisRepository.tieneAsesor(tesis.getId());
        String estado = tesis.getEstado() != null ? tesis.getEstado().name() : null;
        return TemaResponse.builder()
                .tesisId(tesis.getId())
                .estudianteId(estudianteId)
                .titulo(tesis.getTitulo())
                .resumen(tesis.getResumen())
                .lineaInvestigacionId(tesis.getLineaInvestigacion() != null ? tesis.getLineaInvestigacion().getId() : null)
                .lineaNombre(tesis.getLineaInvestigacion() != null ? tesis.getLineaInvestigacion().getNombre() : null)
                .nivel(tesis.getNivel() != null ? tesis.getNivel().name() : null)
                .estado(estado)
                .estadoDerivado(EstadoDerivado.resolver(tesis.getId(), estado, tieneAsesor))
                .fechaRegistro(tesis.getFechaRegistro())
                .build();
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
