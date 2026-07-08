package unmsm.edu.pe.tutorias.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tutorias.application.dto.*;
import unmsm.edu.pe.tutorias.application.mapper.TutoriaMapper;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;
import unmsm.edu.pe.tutorias.domain.services.TutoriaService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class TutoriaServiceImpl implements TutoriaService {

    @Inject TutoriaRepository tutoriaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject TutoriaMapper mapper;
    @Inject unmsm.edu.pe.security.domain.repositories.RoleRepository roleRepository;
    @Inject unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository userRoleRepository;
    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository personaGradoRepository;

    @ConfigProperty(name = "app.tutoria.cupo-default", defaultValue = "20")
    int cupoDefault;

    @Override
    public PageResponse<TutorComboItem> buscarTutores(String buscar, int page, int size) {
        List<TutorComboItem> content = tutoriaRepository.listarTutores(buscar, page, size).stream()
                .map(r -> mapper.toTutorCombo(r, cupoDefault))
                .collect(Collectors.toList());
        return PageResponse.of(content, tutoriaRepository.contarTutores(buscar), page, size);
    }

    @Override
    public PageResponse<EstudianteAsignableItem> estudiantesAsignables(UUID facultadId, UUID programaId, Boolean conTutor, String buscar, int page, int size) {
        List<EstudianteAsignableItem> content = tutoriaRepository
                .listarEstudiantesAsignables(facultadId, programaId, conTutor, buscar, page, size).stream()
                .map(mapper::toEstudianteAsignable)
                .collect(Collectors.toList());
        long total = tutoriaRepository.contarEstudiantesAsignables(facultadId, programaId, conTutor, buscar);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public TutorVigenteItem tutorVigente(UUID estudianteId) {
        return tutoriaRepository.findVigenteByEstudiante(estudianteId).map(t -> {
            Docente d = t.getDocente();
            Persona p = d.getPersona();
            return TutorVigenteItem.builder()
                    .tutoriaId(t.getId())
                    .tutorId(d.getPersonaId())
                    .tutorNombre(nombreDocente(d))
                    .gradoAcademico(personaGradoRepository.gradoPrincipal(d.getPersonaId()))
                    .codigoSistema(d.getCodigoSistema())
                    .estudiantesActuales((int) tutoriaRepository.countActualesByDocente(d.getPersonaId()))
                    .cupoMaximo(effectiveCupo(d))
                    .fechaInicio(t.getFechaInicio())
                    .build();
        }).orElse(null);
    }

    @Override
    @Transactional
    public List<TutoriaHistorialItem> historial(UUID estudianteId) {
        return tutoriaRepository.findByEstudianteId(estudianteId).stream()
                .map(mapper::toHistorialItem)
                .collect(Collectors.toList());
    }

    @Override
    public void asignar(Estudiante estudiante, Docente docente, String motivoCambio) {
        UUID estId = estudiante.getPersonaId();
        UUID docId = docente.getPersonaId();

        var vigente = tutoriaRepository.findVigenteByEstudiante(estId);
        if (vigente.isPresent() && vigente.get().getDocente() != null
                && docId.equals(vigente.get().getDocente().getPersonaId())) {
            return; // idempotente: ya tiene a este mismo tutor
        }

        long actuales = tutoriaRepository.countActualesByDocente(docId);
        int cupo = effectiveCupo(docente);
        if (actuales >= cupo) {
            throw new BusinessException("El tutor " + nombreDocente(docente)
                    + " alcanzó su cupo máximo de tutoría (" + cupo + ")");
        }

        if (vigente.isPresent()) {
            tutoriaRepository.cerrarVigentePorEstudiante(estId, LocalDate.now());
        }

        Tutoria nueva = Tutoria.builder()
                .estudiante(estudiante)
                .docente(docente)
                .fechaInicio(LocalDate.now())
                .actual(true)
                .motivoCambio(trimToNull(motivoCambio))
                .build();
        tutoriaRepository.save(nueva);
    }

    @Override
    @Transactional
    public void asignarIndividual(UUID estudianteId, UUID tutorId, String motivoCambio) {
        Estudiante estudiante = estudianteRepository.findByPersonaId(estudianteId)
                .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + estudianteId));
        Docente docente = docenteRepository.findByPersonaId(tutorId)
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .orElseThrow(() -> new BusinessException("Tutor no encontrado o inactivo: " + tutorId));
        asignar(estudiante, docente, motivoCambio);
        // El docente seleccionado como tutor toma automáticamente el rol PROF_TUTOR (misma transacción).
        habilitarComoTutor(docente);
    }

    @Override
    @Transactional
    public AsignarEnBloqueResponse asignarEnBloque(AsignarEnBloqueRequest request) {
        Docente docente = docenteRepository.findByPersonaId(request.getTutorId())
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .orElseThrow(() -> new BusinessException("Tutor no encontrado o inactivo: " + request.getTutorId()));

        int cupo = effectiveCupo(docente);
        long actuales = tutoriaRepository.countActualesByDocente(docente.getPersonaId());

        List<UUID> ids = request.getEstudianteIds().stream().distinct().collect(Collectors.toList());
        List<Estudiante> aAsignar = new ArrayList<>();
        int sinCambio = 0;
        int reemplazos = 0;

        for (UUID estId : ids) {
            Estudiante est = estudianteRepository.findByPersonaId(estId)
                    .orElseThrow(() -> new NotFoundException("Estudiante no encontrado: " + estId));
            var vigente = tutoriaRepository.findVigenteByEstudiante(estId);
            if (vigente.isPresent() && vigente.get().getDocente() != null
                    && docente.getPersonaId().equals(vigente.get().getDocente().getPersonaId())) {
                sinCambio++;
            } else {
                aAsignar.add(est);
                if (vigente.isPresent()) {
                    reemplazos++;
                }
            }
        }

        int nuevos = aAsignar.size();
        if (actuales + nuevos > cupo) {
            throw new BusinessException(String.format(
                    "El tutor %s no tiene cupo suficiente: máximo %d, ocupados %d, a asignar %d. "
                            + "Reduce la selección o elige otro tutor.",
                    nombreDocente(docente), cupo, actuales, nuevos));
        }

        for (Estudiante est : aAsignar) {
            asignar(est, docente, request.getMotivoCambio());
        }

        // Si el docente recibió su primera tutoría y aún no era tutor, se habilita
        // (rol PROF_TUTOR) en la MISMA transacción. Su cupo queda en el default global
        // hasta que se le configure uno propio (ver effectiveCupo / app.tutoria.cupo-default).
        if (nuevos > 0) {
            habilitarComoTutor(docente);
        }

        int cupoDespues = (int) actuales + nuevos;
        String mensaje = nuevos + " estudiante(s) asignado(s) a " + nombreDocente(docente)
                + (reemplazos > 0 ? " (" + reemplazos + " con reemplazo de tutor)" : "")
                + (sinCambio > 0 ? "; " + sinCambio + " ya lo tenían" : "") + ".";

        return AsignarEnBloqueResponse.builder()
                .asignados(nuevos)
                .sinCambio(sinCambio)
                .reemplazos(reemplazos)
                .cupoMaximo(cupo)
                .cupoAntes((int) actuales)
                .cupoDespues(cupoDespues)
                .mensaje(mensaje)
                .build();
    }

    @Override
    @Transactional
    public void finalizar(UUID docenteId, UUID estudianteId, String motivo) {
        Tutoria vigente = tutoriaRepository.findVigenteByEstudiante(estudianteId)
                .orElseThrow(() -> new BusinessException("El estudiante no tiene un tutor vigente"));
        if (vigente.getDocente() == null || !docenteId.equals(vigente.getDocente().getPersonaId())) {
            throw new BusinessException("La tutoría vigente no corresponde a este tutor");
        }
        vigente.setActual(false);
        vigente.setFechaFin(LocalDate.now());
        if (motivo != null && !motivo.isBlank()) {
            vigente.setMotivoCambio(motivo.trim());
        }
        tutoriaRepository.save(vigente);
    }

    /** Asigna el rol PROF_TUTOR al usuario del docente si aún no lo tiene. */
    private void habilitarComoTutor(Docente docente) {
        Persona p = docente.getPersona();
        unmsm.edu.pe.security.domain.entities.User user = p != null ? p.getUser() : null;
        if (user == null) {
            return; // docente sin cuenta de usuario: no se puede asignar rol
        }
        if (userRoleRepository.findRoleCodesByUserId(user.getId()).contains("PROF_TUTOR")) {
            return; // ya es tutor
        }
        roleRepository.findByCode("PROF_TUTOR").ifPresent(rol ->
                userRoleRepository.save(unmsm.edu.pe.security.domain.entities.UserRoleAssignment.builder()
                        .user(user).role(rol).assigned(true).build()));
    }

    // ── helpers ──
    private int effectiveCupo(Docente d) {
        return d.getCupoMaximoTutoria() != null ? d.getCupoMaximoTutoria() : cupoDefault;
    }

    private String nombreDocente(Docente d) {
        Persona p = d.getPersona();
        if (p == null) {
            return "";
        }
        String ap = (p.getApellidoPaterno() != null ? p.getApellidoPaterno() : "")
                + " " + (p.getApellidoMaterno() != null ? p.getApellidoMaterno() : "");
        return (ap.trim() + ", " + (p.getNombres() != null ? p.getNombres() : "")).trim();
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
