package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.tesis.application.dto.AsesoresCandidatosResponse;
import unmsm.edu.pe.tesis.application.dto.AsesorSugeridoItem;
import unmsm.edu.pe.tesis.application.dto.DocenteComboItem;
import unmsm.edu.pe.tesis.application.dto.SugerirAsesorRequest;
import unmsm.edu.pe.tesis.domain.entities.SugerenciaAsesor;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.SugerenciaAsesorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.SugerenciaAsesorService;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class SugerenciaAsesorServiceImpl implements SugerenciaAsesorService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject DocenteLineaInvestigacionRepository docenteLineaRepository;
    @Inject TutoriaRepository tutoriaRepository;
    @Inject SugerenciaAsesorRepository sugerenciaRepository;
    @Inject AsesoriaRepository asesoriaRepository;
    @Inject TesisRepository tesisRepository;
    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository personaGradoRepository;

    @Override
    @Transactional
    public List<AsesorSugeridoItem> listar(UUID estudianteId) {
        return sugerenciaRepository.listarPorEstudiante(estudianteId).stream()
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AsesorSugeridoItem> listarDeMiTutorando(UUID estudianteId) {
        validarTutorDe(docenteActual(), estudianteId);
        return listar(estudianteId);
    }

    @Override
    @Transactional
    public AsesoresCandidatosResponse candidatosDeMiTutorando(UUID estudianteId) {
        Docente tutor = docenteActual();
        validarTutorDe(tutor, estudianteId);

        // La línea de investigación proviene del tema (tesis) activo del estudiante.
        Tesis tema = tesisRepository.tesisActivaDeEstudiante(estudianteId).orElse(null);
        var linea = tema != null ? tema.getLineaInvestigacion() : null;
        if (linea == null || linea.getId() == null) {
            return AsesoresCandidatosResponse.builder()
                    .conLinea(false)
                    .docentes(List.of())
                    .build();
        }

        UUID tutorId = tutor.getPersonaId();
        List<DocenteComboItem> docentes = docenteLineaRepository.listarDocentesPorLinea(linea.getId()).stream()
                .map(this::toComboItem)
                .filter(d -> d.getId() != null && !d.getId().equals(tutorId)) // el tutor no se sugiere a sí mismo
                .collect(Collectors.toList());

        return AsesoresCandidatosResponse.builder()
                .conLinea(true)
                .lineaId(linea.getId())
                .lineaNombre(linea.getNombre())
                .docentes(docentes)
                .build();
    }

    @Override
    @Transactional
    public AsesorSugeridoItem sugerir(UUID estudianteId, SugerirAsesorRequest request) {
        Docente tutor = docenteActual();
        validarTutorDe(tutor, estudianteId);

        UUID asesorId = request.getAsesorDocenteId();
        docenteRepository.findByPersonaId(asesorId)
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .orElseThrow(() -> new BusinessException("Docente asesor no encontrado o inactivo"));
        if (asesorId.equals(tutor.getPersonaId())) {
            throw new BusinessException("No puedes sugerirte a ti mismo como asesor");
        }
        if (sugerenciaRepository.existe(estudianteId, asesorId)) {
            throw new BusinessException("Ese asesor ya está sugerido para este estudiante");
        }

        SugerenciaAsesor s = sugerenciaRepository.save(SugerenciaAsesor.builder()
                .estudianteId(estudianteId)
                .tutorId(tutor.getPersonaId())
                .asesorDocenteId(asesorId)
                .nota(trimToNull(request.getNota()))
                .build());
        return toItem(s);
    }

    @Override
    @Transactional
    public void quitar(UUID sugerenciaId) {
        SugerenciaAsesor s = sugerenciaRepository.buscarPorId(sugerenciaId)
                .orElseThrow(() -> new NotFoundException("Sugerencia no encontrada: " + sugerenciaId));
        Docente tutor = docenteActual();
        if (!tutor.getPersonaId().equals(s.getTutorId())) {
            throw new BusinessException("No puedes quitar una sugerencia que no es tuya");
        }
        s.setActive(false);
        sugerenciaRepository.save(s);
    }

    // ── helpers ──
    /** Mapea una fila de listarDocentesPorLinea:
     *  [personaId, apellidoPaterno, apellidoMaterno, nombres, codigoSistema, grado]. */
    private DocenteComboItem toComboItem(Object[] r) {
        return DocenteComboItem.builder()
                .id(r[0] instanceof UUID u ? u : (r[0] != null ? UUID.fromString(r[0].toString()) : null))
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .gradoAcademico(asStr(r[5]))
                .build();
    }

    private String asStr(Object o) {
        return o != null ? o.toString() : null;
    }

    private AsesorSugeridoItem toItem(SugerenciaAsesor s) {
        Docente asesor = docenteRepository.findByPersonaId(s.getAsesorDocenteId()).orElse(null);
        Persona p = asesor != null ? asesor.getPersona() : null;
        var docLineas = docenteLineaRepository.findByDocenteId(s.getAsesorDocenteId()).stream()
                .map(unmsm.edu.pe.personas.domain.entities.DocenteLineaInvestigacion::getLineaInvestigacion)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> lineas = docLineas.stream().map(unmsm.edu.pe.personas.domain.entities.LineaInvestigacion::getNombre)
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<UUID> lineaIds = docLineas.stream().map(unmsm.edu.pe.personas.domain.entities.LineaInvestigacion::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        return AsesorSugeridoItem.builder()
                .sugerenciaId(s.getId())
                .asesorDocenteId(s.getAsesorDocenteId())
                .nombres(p != null ? p.getNombres() : null)
                .apellidos(p != null ? join(p.getApellidoPaterno(), p.getApellidoMaterno()) : null)
                .gradoAcademico(asesor != null ? personaGradoRepository.gradoPrincipal(asesor.getPersonaId()) : null)
                .emailInstitucional(asesor != null ? asesor.getEmailInstitucional() : null)
                .lineas(lineas)
                .lineaIds(lineaIds)
                .asesoriasActivas(asesoriaRepository.contarAsesoriasDeDocente(s.getAsesorDocenteId()))
                .nota(s.getNota())
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

    private void validarTutorDe(Docente tutor, UUID estudianteId) {
        Tutoria vigente = tutoriaRepository.findVigenteByEstudiante(estudianteId)
                .orElseThrow(() -> new BusinessException("El estudiante no tiene un tutor vigente"));
        if (vigente.getDocente() == null || !tutor.getPersonaId().equals(vigente.getDocente().getPersonaId())) {
            throw new BusinessException("Solo puedes sugerir asesores a tus propios tutorandos");
        }
    }

    private String join(String a, String b) {
        return ((a != null ? a : "") + " " + (b != null ? b : "")).trim();
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
