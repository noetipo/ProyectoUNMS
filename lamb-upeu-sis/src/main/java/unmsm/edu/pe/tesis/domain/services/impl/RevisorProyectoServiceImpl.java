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
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.*;
import unmsm.edu.pe.tesis.application.util.RubricaDefinicion;
import unmsm.edu.pe.tesis.domain.entities.*;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.repositories.*;
import unmsm.edu.pe.tesis.domain.services.RevisorProyectoService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RevisorProyectoServiceImpl implements RevisorProyectoService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoRevisorRepository revisorRepository;
    @Inject ProyectoRubricaPuntajeRepository puntajeRepository;
    @Inject ProyectoEditorAssembler assembler;

    @Override
    @Transactional
    public List<RevisorBandejaItem> bandeja() {
        UUID docenteId = docenteActual().getPersonaId();
        List<RevisorBandejaItem> out = new ArrayList<>();
        for (Object[] r : revisorRepository.bandejaDeRevisor(docenteId)) {
            out.add(RevisorBandejaItem.builder()
                    .tesisId((UUID) r[0])
                    .proyectoId((UUID) r[1])
                    .estudianteApellidos((asStr(r[5]) + " " + asStr(r[6])).trim())
                    .estudianteNombres(asStr(r[7]))
                    .codigoSistema(asStr(r[8]))
                    .programaNombre(asStr(r[9]))
                    .tituloTesis(asStr(r[10]))
                    .fechaRecepcion(toLocalDate(r[11]))
                    .miEstado(asStr(r[3]))
                    .miPuntaje(r[4] != null ? ((Number) r[4]).intValue() : null)
                    .build());
        }
        return out;
    }

    @Override
    @Transactional
    public EvaluacionRevisorResponse detalle(UUID tesisId) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        ProyectoRevisor rv = revisorDe(p, docente);

        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        ProyectoEditorResponse proyecto = assembler.armar(p, tesis, estudianteDe(tesisId), asesorNombre(tesisId));

        Map<String, Integer> mis = new java.util.HashMap<>();
        for (ProyectoRubricaPuntaje pj : puntajeRepository.listarPorRevisor(rv.getId())) {
            mis.put(pj.getCriterio(), pj.getPuntaje());
        }
        List<RubricaCriterioItem> rubrica = RubricaDefinicion.CRITERIOS.stream()
                .map(c -> RubricaCriterioItem.builder()
                        .key(c.key()).titulo(c.titulo()).descripcion(c.descripcion())
                        .puntaje(mis.get(c.key())).build())
                .toList();

        return EvaluacionRevisorResponse.builder()
                .proyecto(proyecto)
                .rubrica(rubrica)
                .miEstado(rv.getEstado() != null ? rv.getEstado().name() : null)
                .miComentario(rv.getComentario())
                .miPuntajeTotal(rv.getPuntajeTotal())
                .puntajeMaximo(RubricaDefinicion.puntajeMaximo())
                .cerrada(rv.getEstado() == EstadoRevisor.CONFORME)
                .respuestaEstudiante(rv.getRespuestaEstudiante())
                .build();
    }

    @Override
    @Transactional
    public void evaluar(UUID tesisId, EvaluarRevisorRequest req) {
        Docente docente = docenteActual();
        ProyectoTesis p = proyecto(tesisId);
        ProyectoRevisor rv = revisorDe(p, docente);
        if (rv.getEstado() == EstadoRevisor.CONFORME) {
            throw new BusinessException("Ya diste conformidad a este proyecto; la evaluación está cerrada");
        }
        Map<String, Integer> puntajes = req != null ? req.getPuntajes() : null;
        if (puntajes == null || puntajes.isEmpty()) {
            throw new ValidationException("Completa la rúbrica antes de evaluar");
        }
        // Todos los criterios deben tener un puntaje válido (1..4).
        int total = 0;
        for (RubricaDefinicion.Criterio c : RubricaDefinicion.CRITERIOS) {
            Integer v = puntajes.get(c.key());
            if (v == null) {
                throw new ValidationException("Falta calificar: " + c.titulo());
            }
            if (v < RubricaDefinicion.PUNTAJE_MIN || v > RubricaDefinicion.PUNTAJE_MAX) {
                throw new ValidationException("Puntaje fuera de rango en: " + c.titulo());
            }
            total += v;
        }
        // Observar exige comentario; dar conformidad no.
        if (!req.isConforme() && (req.getComentario() == null || req.getComentario().isBlank())) {
            throw new ValidationException("Escribe las observaciones para el estudiante");
        }

        // Reemplaza los puntajes previos por los nuevos.
        puntajeRepository.eliminarPorRevisor(rv.getId());
        for (RubricaDefinicion.Criterio c : RubricaDefinicion.CRITERIOS) {
            puntajeRepository.save(ProyectoRubricaPuntaje.builder()
                    .revisorId(rv.getId()).criterio(c.key()).puntaje(puntajes.get(c.key())).build());
        }

        rv.setPuntajeTotal(total);
        rv.setComentario(req.getComentario());
        if (req.isConforme()) {
            rv.setEstado(EstadoRevisor.CONFORME);
            rv.setFechaConformidad(LocalDate.now());
        } else {
            rv.setEstado(EstadoRevisor.OBSERVADO);
            // nueva observación: se borra el levantamiento previo del estudiante
            rv.setRespuestaEstudiante(null);
            rv.setFechaRespuesta(null);
        }
        revisorRepository.save(rv);

        // Si ambos revisores dieron conformidad, el proyecto queda aprobado para la defensa.
        if (req.isConforme()) {
            java.util.List<ProyectoRevisor> todos = revisorRepository.listarPorProyecto(p.getId());
            boolean todosConformes = !todos.isEmpty()
                    && todos.stream().allMatch(x -> x.getEstado() == EstadoRevisor.CONFORME);
            if (todosConformes && !Boolean.TRUE.equals(p.getRevisoresConformes())) {
                p.setRevisoresConformes(true);
                p.setFechaRevisoresConformes(LocalDate.now());
                proyectoRepository.save(p);
            }
        }
    }

    // ── helpers ──
    private ProyectoTesis proyecto(UUID tesisId) {
        return proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
    }

    private ProyectoRevisor revisorDe(ProyectoTesis p, Docente docente) {
        return revisorRepository.buscarPorProyectoYDocente(p.getId(), docente.getPersonaId())
                .orElseThrow(() -> new BusinessException("No eres revisor de este proyecto"));
    }

    private Estudiante estudianteDe(UUID tesisId) {
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        return estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
    }

    private String asesorNombre(UUID tesisId) {
        return proyectoRepository.buscarPorTesisId(tesisId)
                .map(ProyectoTesis::getAsesorId)
                .flatMap(id -> docenteRepository.findByPersonaId(id))
                .map(d -> nombre(d.getPersona()))
                .orElse(null);
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

    private java.time.LocalDate toLocalDate(Object o) {
        if (o instanceof java.time.LocalDate ld) return ld;
        if (o instanceof java.sql.Date sd) return sd.toLocalDate();
        return null;
    }

    private String asStr(Object o) { return o != null ? o.toString() : null; }

    private String nz(String s) { return s == null ? "" : s; }
}
