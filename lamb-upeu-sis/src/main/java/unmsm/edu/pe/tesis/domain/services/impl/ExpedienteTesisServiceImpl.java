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
import unmsm.edu.pe.shared.utils.EstadoDerivado;
import unmsm.edu.pe.tesis.application.dto.EtapaItem;
import unmsm.edu.pe.tesis.application.util.EtapasProceso;
import unmsm.edu.pe.tesis.application.dto.ExpedienteResponse;
import unmsm.edu.pe.tesis.domain.entities.DictamenDesignacion;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoDictamen;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.DictamenDesignacionRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.ExpedienteTesisService;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class ExpedienteTesisServiceImpl implements ExpedienteTesisService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject TesisRepository tesisRepository;
    @Inject TesisAutorRepository tesisAutorRepository;
    @Inject AsesoriaRepository asesoriaRepository;
    @Inject DictamenDesignacionRepository dictamenRepository;
    @Inject TutoriaRepository tutoriaRepository;
    @Inject unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository proyectoRepository;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("es"));

    @Override
    @Transactional
    public ExpedienteResponse miExpediente() {
        Estudiante est = estudianteActual();
        Tesis tesis = tesisRepository.tesisActivaDeEstudiante(est.getPersonaId())
                .orElseThrow(() -> new BusinessException("No tienes una tesis activa"));
        return construir(tesis, est);
    }

    @Override
    @Transactional
    public ExpedienteResponse expediente(UUID tesisId) {
        Tesis tesis = tesisRepository.buscarPorId(tesisId)
                .orElseThrow(() -> new NotFoundException("Tesis no encontrada"));
        UUID estId = tesisAutorRepository.estudianteDeTesis(tesisId);
        Estudiante est = estId != null ? estudianteRepository.findByPersonaId(estId).orElse(null) : null;
        return construir(tesis, est);
    }

    private ExpedienteResponse construir(Tesis tesis, Estudiante est) {
        UUID tesisId = tesis.getId();
        Persona pe = est != null ? est.getPersona() : null;
        ProgramaPosgrado prog = est != null ? est.getPrograma() : null;

        boolean tieneAsesor = tesisRepository.tieneAsesor(tesisId);
        String estadoTesis = tesis.getEstado() != null ? tesis.getEstado().name() : null;
        String estadoDerivado = EstadoDerivado.resolver(tesisId, estadoTesis, tieneAsesor);

        // Etapa 2: tutor
        Tutoria tutoria = est != null
                ? tutoriaRepository.findVigenteByEstudiante(est.getPersonaId()).orElse(null) : null;
        Docente tutor = tutoria != null ? tutoria.getDocente() : null;

        // Etapa 3: dictamen de designación de asesor
        DictamenDesignacion dictamen = dictamenRepository.buscarPorTesisId(tesisId).orElse(null);
        boolean dictamenFirmado = dictamen != null && dictamen.getEstado() == EstadoDictamen.FIRMADO;

        // Asesor
        String asesorNombre = asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .flatMap(a -> docenteRepository.findByPersonaId(a.getDocenteId()))
                .map(d -> nombre(d.getPersona())).orElse(null);

        // ── Etapa EN CURSO (1..9; 9 = todo completado) ──
        int enCurso;
        if (!dictamenFirmado) {
            enCurso = !tieneAsesor && !dictamenFirmado
                    ? (tutor == null ? 2 : 3)
                    : 3;
            if (tesis.getEstado() == null) enCurso = 1;
        } else {
            enCurso = switch (estadoTesis != null ? estadoTesis : "TEMA_REGISTRADO") {
                case "PROYECTO_PRESENTADO" -> 5;
                case "PROYECTO_APROBADO" -> 6;
                case "EN_DESARROLLO" -> 7;
                case "SUSTENTADO" -> 9;
                default -> 4; // TEMA_REGISTRADO con dictamen firmado → Etapa 4 en curso
            };
        }

        // ── Construcción de las 8 etapas ──
        List<EtapaItem> etapas = new ArrayList<>();
        for (int i = 0; i < EtapasProceso.TOTAL; i++) {
            int numero = i + 1;
            String estado = numero < enCurso ? "COMPLETADO" : (numero == enCurso ? "EN_CURSO" : "PENDIENTE");
            String fecha = switch (numero) {
                case 1 -> tesis.getFechaRegistro() != null ? tesis.getFechaRegistro().format(FECHA) : null;
                case 2 -> tutoria != null && tutoria.getFechaInicio() != null ? tutoria.getFechaInicio().format(FECHA) : null;
                case 3 -> dictamen != null && dictamen.getFechaEmision() != null ? dictamen.getFechaEmision().format(FECHA) : null;
                default -> null;
            };
            boolean tieneDictamen = numero == 3 && dictamenFirmado;
            etapas.add(EtapaItem.builder()
                    .numero(numero)
                    .titulo(EtapasProceso.titulo(numero))
                    .descripcion(EtapasProceso.descripcion(numero))
                    .estado(estado)
                    .fecha(fecha)
                    .tieneDictamen(tieneDictamen)
                    .dictamenLabel(tieneDictamen ? "Dictamen designación de asesor" : null)
                    .build());
        }

        int avancePct = EtapasProceso.avancePct(enCurso);

        return ExpedienteResponse.builder()
                .tesisId(tesisId)
                .codigo(est != null ? est.getCodigoSistema() : null)
                .estadoDerivado(estadoDerivado)
                .estadoLabel(EstadoDerivado.etiqueta(estadoDerivado))
                .titulo(tesis.getTitulo())
                .doctorandoNombre(nombre(pe))
                .programaNombre(prog != null ? prog.getNombre() : null)
                .asesorNombre(asesorNombre)
                .tutorNombre(tutor != null ? nombre(tutor.getPersona()) : null)
                .lineaNombre(tesis.getLineaInvestigacion() != null ? tesis.getLineaInvestigacion().getNombre() : null)
                .avancePct(avancePct)
                .etapaEnCurso(enCurso)
                // La Etapa 4 (elaboración del proyecto) arranca con el dictamen de designación firmado.
                .proyectoHabilitado(enCurso >= 4)
                // Con la carta del asesor emitida, la redacción terminó: toca el cierre del expediente.
                .cierreHabilitado(proyectoRepository.buscarPorTesisId(tesisId)
                        .map(pr -> Boolean.TRUE.equals(pr.getCartaAsesor())).orElse(false))
                .etapas(etapas)
                .build();
    }

    private Estudiante estudianteActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        Persona persona = personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
        return estudianteRepository.findByPersonaId(persona.getId())
                .orElseThrow(() -> new BusinessException("El usuario autenticado no tiene perfil de estudiante"));
    }

    private String nombre(Persona p) {
        if (p == null) return null;
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno()))
                .trim().replaceAll("\\s+", " ");
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
