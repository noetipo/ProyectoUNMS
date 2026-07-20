package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.*;
import unmsm.edu.pe.tesis.domain.entities.InformeRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.repositories.*;
import unmsm.edu.pe.tesis.domain.services.InformeJuradoService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class InformeJuradoServiceImpl implements InformeJuradoService {

    private static final int PUNTAJE_MAX = 20;
    private static final String T_INFORME_FINAL = "INFORME_FINAL_TESIS";

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject TesisRepository tesisRepository;
    @Inject InformeRevisorRepository informeRevisorRepository;
    @Inject DocumentoTesisRepository documentoTesisRepository;

    @Override
    @Transactional
    public List<InformeJuradoBandejaItem> bandeja() {
        UUID docenteId = docenteActual().getPersonaId();
        List<InformeJuradoBandejaItem> out = new ArrayList<>();
        for (Object[] r : informeRevisorRepository.bandejaDeJurado(docenteId)) {
            out.add(InformeJuradoBandejaItem.builder()
                    .tesisId((UUID) r[0])
                    .miEstado(asStr(r[3]))
                    .presidente(Boolean.TRUE.equals(r[4]))
                    .estudianteApellidos((asStr(r[5]) + " " + asStr(r[6])).trim())
                    .estudianteNombres(asStr(r[7]))
                    .codigoSistema(asStr(r[8]))
                    .programaNombre(asStr(r[9]))
                    .tituloTesis(asStr(r[10]))
                    .build());
        }
        return out;
    }

    @Override
    @Transactional
    public InformeEvaluacionResponse detalle(UUID tesisId) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        InformeRevisor rv = revisorDe(p, docente);
        Estudiante est = estudianteDe(tesisId);
        Persona pe = est != null ? est.getPersona() : null;
        ProgramaPosgrado prog = est != null ? est.getPrograma() : null;

        return InformeEvaluacionResponse.builder()
                .estudianteNombre(nombre(pe))
                .codigoSistema(est != null ? est.getCodigoSistema() : null)
                .programaNombre(prog != null ? prog.getNombre() : null)
                .titulo(tesisTitulo(tesisId))
                .informeFinalSubido(documentoTesisRepository.existePorTesisYTipo(tesisId, T_INFORME_FINAL))
                .presidente(Boolean.TRUE.equals(rv.getPresidente()))
                .miEstado(rv.getEstado() != null ? rv.getEstado().name() : null)
                .miPuntaje(rv.getPuntaje())
                .miComentario(rv.getComentario())
                .respuestaEstudiante(rv.getRespuestaEstudiante())
                .cerrada(rv.getEstado() == EstadoRevisor.CONFORME)
                .puntajeMaximo(PUNTAJE_MAX)
                .build();
    }

    @Override
    @Transactional
    public void evaluar(UUID tesisId, EvaluarInformeRequest req) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        InformeRevisor rv = revisorDe(p, docente);
        if (rv.getEstado() == EstadoRevisor.CONFORME) {
            throw new BusinessException("Ya diste conformidad al informe final; la evaluación está cerrada");
        }
        if (req == null || req.getPuntaje() == null) {
            throw new ValidationException("Indica el puntaje del informe (0 a " + PUNTAJE_MAX + ")");
        }
        if (req.getPuntaje() < 0 || req.getPuntaje() > PUNTAJE_MAX) {
            throw new ValidationException("El puntaje debe estar entre 0 y " + PUNTAJE_MAX);
        }
        if (!req.isConforme() && (req.getComentario() == null || req.getComentario().isBlank())) {
            throw new ValidationException("Escribe las observaciones para el estudiante");
        }

        rv.setPuntaje(req.getPuntaje());
        rv.setComentario(req.getComentario());
        if (req.isConforme()) {
            rv.setEstado(EstadoRevisor.CONFORME);
            rv.setFechaConformidad(LocalDate.now());
        } else {
            rv.setEstado(EstadoRevisor.OBSERVADO);
            rv.setRespuestaEstudiante(null);
        }
        informeRevisorRepository.save(rv);

        // Si los 3 miembros dieron conformidad, el informe final queda aprobado por el Jurado Informante.
        if (req.isConforme()) {
            List<InformeRevisor> todos = informeRevisorRepository.listarPorProyecto(p.getId());
            boolean todosConformes = !todos.isEmpty()
                    && todos.stream().allMatch(x -> x.getEstado() == EstadoRevisor.CONFORME);
            if (todosConformes && !Boolean.TRUE.equals(p.getInformeFinalRevisado())) {
                p.setInformeFinalRevisado(true);
                p.setFechaInformeRevisado(LocalDate.now());
                proyectoRepository.save(p);
            }
        }
    }

    // ── helpers ──
    private ProyectoTesis proyecto(UUID tesisId) {
        return proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
    }

    private InformeRevisor revisorDe(ProyectoTesis p, Docente docente) {
        return informeRevisorRepository.buscarPorProyectoYDocente(p.getId(), docente.getPersonaId())
                .orElseThrow(() -> new BusinessException("No integras el Jurado Informante de esta tesis"));
    }

    private Estudiante estudianteDe(UUID tesisId) {
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        return estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
    }

    private String tesisTitulo(UUID tesisId) {
        return tesisRepository.buscarPorId(tesisId)
                .map(unmsm.edu.pe.tesis.domain.entities.Tesis::getTitulo).orElse(null);
    }

    private Docente docenteActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) throw new BusinessException("Usuario no autenticado");
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
