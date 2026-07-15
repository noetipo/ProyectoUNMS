package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.ProyectoBandejaItem;
import unmsm.edu.pe.tesis.application.dto.ProyectoEditorResponse;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.TutorProyectoService;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class TutorProyectoServiceImpl implements TutorProyectoService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject AsesoriaRepository asesoriaRepository;
    @Inject TutoriaRepository tutoriaRepository;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoEditorAssembler assembler;

    @Override
    @Transactional
    public PageResponse<ProyectoBandejaItem> bandeja(String buscar, int page, int size) {
        Docente tutor = docenteActual();
        List<ProyectoBandejaItem> content = proyectoRepository
                .bandejaDeTutor(tutor.getPersonaId(), buscar, page, size)
                .stream().map(this::toBandejaItem).collect(Collectors.toList());
        long total = proyectoRepository.contarBandejaDeTutor(tutor.getPersonaId(), buscar);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public ProyectoEditorResponse detalle(UUID tesisId) {
        Docente tutor = docenteActual();
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("El proyecto aún no ha sido iniciado por el estudiante"));
        Estudiante est = estudianteDe(tesisId);
        verificarTutor(est, tutor);
        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        return assembler.armar(p, tesis, est, asesorNombre(tesisId));
    }

    // ── helpers ──
    private void verificarTutor(Estudiante est, Docente tutor) {
        if (est == null) {
            throw new BusinessException("La tesis no tiene estudiante asociado");
        }
        Tutoria tutoria = tutoriaRepository.findVigenteByEstudiante(est.getPersonaId()).orElse(null);
        UUID tutorId = tutoria != null && tutoria.getDocente() != null ? tutoria.getDocente().getPersonaId() : null;
        if (tutorId == null || !tutorId.equals(tutor.getPersonaId())) {
            throw new BusinessException("No eres el tutor de este estudiante");
        }
    }

    private Estudiante estudianteDe(UUID tesisId) {
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        return estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
    }

    private String asesorNombre(UUID tesisId) {
        return asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .flatMap(a -> docenteRepository.findByPersonaId(a.getDocenteId()))
                .map(d -> nombre(d.getPersona()))
                .orElse(null);
    }

    private ProyectoBandejaItem toBandejaItem(Object[] r) {
        return ProyectoBandejaItem.builder()
                .tesisId((UUID) r[0])
                .proyectoId((UUID) r[1])
                .estado(asStr(r[2]))
                .estudianteApellidos((asStr(r[3]) + " " + asStr(r[4])).trim())
                .estudianteNombres(asStr(r[5]))
                .codigoSistema(asStr(r[6]))
                .programaNombre(asStr(r[7]))
                .tituloTesis(asStr(r[8]))
                .build();
    }

    private Docente docenteActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        Persona persona = personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
        return docenteRepository.findByPersonaId(persona.getId())
                .orElseThrow(() -> new BusinessException("El usuario autenticado no tiene perfil de docente"));
    }

    private String nombre(Persona p) {
        if (p == null) return null;
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno()))
                .trim().replaceAll("\\s+", " ");
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }
    private String nz(String s) { return s == null ? "" : s; }
}
