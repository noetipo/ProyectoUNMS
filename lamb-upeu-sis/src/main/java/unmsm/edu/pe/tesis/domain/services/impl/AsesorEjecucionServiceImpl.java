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
import unmsm.edu.pe.tesis.application.util.RubricaAvanceDefinicion;
import unmsm.edu.pe.tesis.domain.entities.*;
import unmsm.edu.pe.tesis.domain.enums.EstadoActividad;
import unmsm.edu.pe.tesis.domain.repositories.*;
import unmsm.edu.pe.tesis.domain.services.AsesorEjecucionService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AsesorEjecucionServiceImpl implements AsesorEjecucionService {

    private static final String T_INFORME_FINAL = "INFORME_FINAL_TESIS";

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject ProyectoActividadRepository actividadRepository;
    @Inject ProyectoAvanceRepository avanceRepository;
    @Inject DocumentoTesisRepository documentoTesisRepository;
    @Inject ProyectoEditorAssembler assembler;
    @Inject AsesorDesignadoService asesorDesignado;

    @Override
    @Transactional
    public List<EjecucionBandejaItem> bandeja() {
        Docente asesor = docenteActual();
        List<EjecucionBandejaItem> out = new ArrayList<>();
        for (Object[] r : proyectoRepository.bandejaDeAsesor(asesor.getPersonaId(), null, null, 0, 50)) {
            UUID tesisId = (UUID) r[0];
            UUID proyectoId = (UUID) r[1];
            ProyectoTesis p = proyectoRepository.buscarPorId(proyectoId).orElse(null);
            if (p == null || !Boolean.TRUE.equals(p.getDefensaProgramada())) {
                continue; // solo proyectos en ejecución
            }
            out.add(EjecucionBandejaItem.builder()
                    .tesisId(tesisId).proyectoId(proyectoId)
                    .estudianteApellidos((asStr(r[3]) + " " + asStr(r[4])).trim())
                    .estudianteNombres(asStr(r[5]))
                    .codigoSistema(asStr(r[6]))
                    .programaNombre(asStr(r[7]))
                    .tituloTesis(asStr(r[8]))
                    .porcentajePlan(porcentajePlan(proyectoId))
                    .informeFinalAprobado(Boolean.TRUE.equals(p.getInformeFinalAprobado()))
                    .numAvances(avanceRepository.listarPorProyecto(proyectoId).size())
                    .build());
        }
        return out;
    }

    @Override
    @Transactional
    public EjecucionDetalleResponse detalle(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId, docenteActual());
        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        ProyectoEditorResponse proyecto = assembler.armar(p, tesis, estudianteDe(tesisId), null);

        int pct = porcentajePlan(p.getId());
        List<AvanceItem> avances = avanceRepository.listarPorProyecto(p.getId()).stream()
                .map(this::toAvance).toList();

        return EjecucionDetalleResponse.builder()
                .proyecto(proyecto)
                .avances(avances)
                .porcentajePlan(pct)
                .planCompleto(pct >= 100)
                .informeFinalAprobado(Boolean.TRUE.equals(p.getInformeFinalAprobado()))
                .informeFinalSubido(documentoTesisRepository.existePorTesisYTipo(tesisId, T_INFORME_FINAL))
                .puntajeMaximo(RubricaAvanceDefinicion.puntajeMaximo())
                .build();
    }

    @Override
    @Transactional
    public void registrarAvance(UUID tesisId, RegistrarAvanceRequest req) {
        ProyectoTesis p = proyecto(tesisId, docenteActual());
        Map<String, Integer> puntajes = req != null ? req.getPuntajes() : null;
        if (puntajes == null || puntajes.isEmpty()) {
            throw new ValidationException("Completa la rúbrica de avance");
        }
        for (RubricaAvanceDefinicion.Criterio c : RubricaAvanceDefinicion.CRITERIOS) {
            Integer v = puntajes.get(c.key());
            if (v == null) throw new ValidationException("Falta calificar: " + c.titulo());
            if (v < RubricaAvanceDefinicion.PUNTAJE_MIN || v > RubricaAvanceDefinicion.PUNTAJE_MAX) {
                throw new ValidationException("Puntaje fuera de rango en: " + c.titulo());
            }
        }
        avanceRepository.save(ProyectoAvance.builder()
                .proyectoId(p.getId())
                .fechaEvaluacion(LocalDate.now())
                .puntajeEjecucion(puntajes.get("ejecucion"))
                .puntajeDatos(puntajes.get("datos"))
                .puntajeAnalisis(puntajes.get("analisis"))
                .puntajeInterpretacion(puntajes.get("interpretacion"))
                .porcentajePlan(porcentajePlan(p.getId()))
                .comentario(req.getComentario())
                .build());
    }

    @Override
    @Transactional
    public void aprobarInformeFinal(UUID tesisId) {
        ProyectoTesis p = proyecto(tesisId, docenteActual());
        if (Boolean.TRUE.equals(p.getInformeFinalAprobado())) {
            throw new BusinessException("El informe final ya fue aprobado");
        }
        if (porcentajePlan(p.getId()) < 100) {
            throw new BusinessException("El plan de actividades aún no está 100% ejecutado");
        }
        if (!documentoTesisRepository.existePorTesisYTipo(tesisId, T_INFORME_FINAL)) {
            throw new BusinessException("El estudiante aún no ha subido el informe final");
        }
        p.setInformeFinalAprobado(true);
        p.setFechaInformeFinal(LocalDate.now());
        proyectoRepository.save(p);
    }

    // ── helpers ──
    private int porcentajePlan(UUID proyectoId) {
        List<ProyectoActividad> acts = actividadRepository.listarPorProyecto(proyectoId);
        if (acts.isEmpty()) return 0;
        long hechas = acts.stream().filter(a -> a.getEstado() == EstadoActividad.HECHA).count();
        return (int) Math.round((hechas * 100.0) / acts.size());
    }

    private AvanceItem toAvance(ProyectoAvance a) {
        int total = nz(a.getPuntajeEjecucion()) + nz(a.getPuntajeDatos())
                + nz(a.getPuntajeAnalisis()) + nz(a.getPuntajeInterpretacion());
        return AvanceItem.builder()
                .id(a.getId()).fechaEvaluacion(a.getFechaEvaluacion())
                .puntajeEjecucion(a.getPuntajeEjecucion()).puntajeDatos(a.getPuntajeDatos())
                .puntajeAnalisis(a.getPuntajeAnalisis()).puntajeInterpretacion(a.getPuntajeInterpretacion())
                .puntajeTotal(total).porcentajePlan(a.getPorcentajePlan())
                .comentario(a.getComentario()).build();
    }

    private ProyectoTesis proyecto(UUID tesisId, Docente asesor) {
        ProyectoTesis p = proyectoRepository.buscarPorTesisId(tesisId)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado"));
        // La designación vive en `asesorias`; asesor_id es una copia que puede estar vacía.
        UUID asesorVigente = asesorDesignado.sincronizar(p);
        if (asesorVigente == null || !asesorVigente.equals(asesor.getPersonaId())) {
            throw new BusinessException("No eres el asesor de esta tesis");
        }
        return p;
    }

    private Estudiante estudianteDe(UUID tesisId) {
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        return estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
    }

    private Docente docenteActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) throw new BusinessException("Usuario no autenticado");
        Persona persona = personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
        return docenteRepository.findByPersonaId(persona.getId())
                .orElseThrow(() -> new BusinessException("El usuario autenticado no tiene perfil de docente"));
    }

    private int nz(Integer i) { return i != null ? i : 0; }

    private String asStr(Object o) { return o != null ? o.toString() : null; }
}
