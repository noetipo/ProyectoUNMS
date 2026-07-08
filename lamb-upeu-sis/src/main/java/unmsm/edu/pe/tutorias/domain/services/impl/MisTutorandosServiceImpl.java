package unmsm.edu.pe.tutorias.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tutorias.application.dto.MisTutorandosResumen;
import unmsm.edu.pe.tutorias.application.dto.TutorEstudianteItem;
import unmsm.edu.pe.tutorias.application.mapper.ReporteTutoresMapper;
import unmsm.edu.pe.tutorias.domain.repositories.ReporteTutoresRepository;
import unmsm.edu.pe.tutorias.domain.services.MisTutorandosService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class MisTutorandosServiceImpl implements MisTutorandosService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject ReporteTutoresRepository repo;
    @Inject ReporteTutoresMapper mapper;

    @ConfigProperty(name = "app.tutoria.cupo-default", defaultValue = "20")
    int cupoDefault;

    @Override
    public MisTutorandosResumen resumen() {
        Persona persona = personaActual();
        Docente docente = docenteActual(persona);
        long total = repo.contarEstudiantesDeTutor(persona.getId());
        int cupo = docente.getCupoMaximoTutoria() != null ? docente.getCupoMaximoTutoria() : cupoDefault;
        String nombre = (join(persona.getApellidoPaterno(), persona.getApellidoMaterno())
                + ", " + (persona.getNombres() != null ? persona.getNombres() : "")).trim();
        return MisTutorandosResumen.builder()
                .tutorNombre(nombre)
                .total(total)
                .cupoMaximo(cupo)
                .build();
    }

    @Override
    public PageResponse<TutorEstudianteItem> tutorandos(String buscar, int page, int size) {
        Persona persona = personaActual();
        docenteActual(persona); // valida que el usuario sea docente
        UUID docenteId = persona.getId();
        List<TutorEstudianteItem> content = repo.tutorandos(docenteId, buscar, page, size).stream()
                .map(mapper::toTutorandoItem)
                .collect(Collectors.toList());
        return PageResponse.of(content, repo.contarTutorandos(docenteId, buscar), page, size);
    }

    // ── helpers ──
    private Persona personaActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        return personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
    }

    private Docente docenteActual(Persona persona) {
        return docenteRepository.findByPersonaId(persona.getId())
                .orElseThrow(() -> new BusinessException("El usuario autenticado no tiene perfil de docente"));
    }

    private String join(String a, String b) {
        return ((a != null ? a : "") + " " + (b != null ? b : "")).trim();
    }
}
