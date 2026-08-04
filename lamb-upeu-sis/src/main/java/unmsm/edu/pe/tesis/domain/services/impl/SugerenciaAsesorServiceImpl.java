package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.PersonaGradoAcademico;
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
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.SugerenciaAsesorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.SugerenciaAsesorService;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;

import java.util.Comparator;
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
    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaCargoRepository personaCargoRepository;
    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaCentroLaboralRepository personaCentroLaboralRepository;

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
                .asesorActual(nombreDeAsesoria(tema.getId(), "ASESOR"))
                .coasesorActual(nombreDeAsesoria(tema.getId(), "COASESOR"))
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

        // La tesis lleva UN asesor y, opcionalmente, UN co-asesor: se sugiere un docente por
        // puesto. Para proponer a otro hay que quitar antes la sugerencia vigente de ese puesto.
        TipoAsesoria tipo = request.getTipo() != null ? request.getTipo() : TipoAsesoria.ASESOR;
        boolean puestoOcupado = sugerenciaRepository.listarPorEstudiante(estudianteId).stream()
                .anyMatch(x -> x.tipoEfectivo() == tipo);
        if (puestoOcupado) {
            throw new BusinessException(tipo == TipoAsesoria.COASESOR
                    ? "Ya hay un co-asesor sugerido para este estudiante; quítalo si deseas proponer a otro"
                    : "Ya hay un asesor sugerido para este estudiante; quítalo si deseas proponer a otro");
        }

        SugerenciaAsesor s = sugerenciaRepository.save(SugerenciaAsesor.builder()
                .estudianteId(estudianteId)
                .tutorId(tutor.getPersonaId())
                .asesorDocenteId(asesorId)
                .tipo(tipo)
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
     *  [personaId, apPaterno, apMaterno, nombres, codigoSistema, grado, categoria, condicion, email]. */
    private DocenteComboItem toComboItem(Object[] r) {
        UUID personaId = r[0] instanceof UUID u ? u : (r[0] != null ? UUID.fromString(r[0].toString()) : null);
        return DocenteComboItem.builder()
                .id(personaId)
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .gradoAcademico(asStr(r[5]))
                .categoria(asStr(r[6]))
                .condicion(asStr(r[7]))
                .cargoActual(cargoActual(personaId))
                .estudios(estudios(personaId))
                .asesoriasActivas(personaId != null ? asesoriaRepository.contarAsesoriasDeDocente(personaId) : 0)
                .emailInstitucional(asStr(r[8]))
                .lineas(lineasDe(personaId))
                .centroLaboral(centroLaboral(personaId, false))
                .centroLaboralDetalle(centroLaboral(personaId, true))
                .experienciaAnios(experienciaAnios(personaId))
                .orcid(personaId != null ? personaRepository.buscarPorId(personaId).map(Persona::getOrcid).orElse(null) : null)
                .build();
    }

    /** Nombre del docente con la asesoría vigente del tipo pedido (ASESOR | COASESOR), o null. */
    private String nombreDeAsesoria(UUID tesisId, String tipo) {
        return asesoriaRepository.buscarPorTesisYTipo(tesisId, tipo)
                .map(a -> docenteRepository.findByPersonaId(a.getDocenteId()).orElse(null))
                .filter(Objects::nonNull)
                .map(d -> {
                    Persona p = d.getPersona();
                    return p != null ? join(p.getApellidoPaterno(), p.getApellidoMaterno()) + ", " + p.getNombres() : null;
                })
                .orElse(null);
    }

    /** Nombres de las líneas de investigación registradas del docente. */
    private List<String> lineasDe(UUID personaId) {
        if (personaId == null) {
            return List.of();
        }
        return docenteLineaRepository.findByDocenteId(personaId).stream()
                .map(unmsm.edu.pe.personas.domain.entities.DocenteLineaInvestigacion::getLineaInvestigacion)
                .filter(Objects::nonNull)
                .map(unmsm.edu.pe.personas.domain.entities.LineaInvestigacion::getNombre)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Cargo vigente de la persona (el primero marcado como actual), o null. */
    /** Centro laboral vigente (o el más reciente): {@code detalle=false} el nombre, {@code true} su descripción. */
    private String centroLaboral(UUID personaId, boolean detalle) {
        if (personaId == null) {
            return null;
        }
        var centros = personaCentroLaboralRepository.findByPersonaId(personaId);
        return centros.stream()
                .filter(pc -> pc.getCentroLaboral() != null)
                .sorted(Comparator.comparing((unmsm.edu.pe.personas.domain.entities.PersonaCentroLaboral pc)
                                -> !Boolean.TRUE.equals(pc.getActual()))
                        .thenComparing(pc -> pc.getFechaInicio() == null ? java.time.LocalDate.MIN : pc.getFechaInicio(),
                                Comparator.reverseOrder()))
                .map(pc -> detalle ? pc.getCentroLaboral().getDescripcion() : pc.getCentroLaboral().getNombre())
                .filter(v -> v != null && !v.isBlank())
                .findFirst().orElse(null);
    }

    /** Años de experiencia: desde el inicio de su vínculo laboral más antiguo registrado. */
    private Integer experienciaAnios(UUID personaId) {
        if (personaId == null) {
            return null;
        }
        return personaCentroLaboralRepository.findByPersonaId(personaId).stream()
                .map(pc -> pc.getFechaInicio())
                .filter(Objects::nonNull)
                .min(java.time.LocalDate::compareTo)
                .map(desde -> (int) java.time.temporal.ChronoUnit.YEARS.between(desde, java.time.LocalDate.now()))
                .filter(a -> a > 0)
                .orElse(null);
    }

    private String categoriaLabel(String cat) {
        return switch (cat) {
            case "PRINCIPAL" -> "Docente Principal";
            case "ASOCIADO" -> "Docente Asociado";
            case "AUXILIAR" -> "Docente Auxiliar";
            default -> cat;
        };
    }

    private String condicionLabel(String cond) {
        return switch (cond) {
            case "NOMBRADO" -> "Nombrado";
            case "CONTRATADO" -> "Contratado";
            default -> cond;
        };
    }

    private String cargoActual(UUID personaId) {
        if (personaId == null) {
            return null;
        }
        return personaCargoRepository.findByPersonaId(personaId).stream()
                .filter(pc -> Boolean.TRUE.equals(pc.getActual()) && pc.getCargo() != null)
                .map(pc -> pc.getCargo().getNombre())
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse(null);
    }

    /** Grados académicos formateados ("Doctor — UNMSM (2015)"), el principal primero. */
    private List<String> estudios(UUID personaId) {
        if (personaId == null) {
            return List.of();
        }
        return personaGradoRepository.findByPersonaId(personaId).stream()
                .filter(g -> g.getGrado() != null)
                .sorted(Comparator.comparing((PersonaGradoAcademico g) -> !Boolean.TRUE.equals(g.getPrincipal()))
                        .thenComparing(g -> g.getAnio() == null ? 0 : -g.getAnio()))
                .map(this::formatearGrado)
                .toList();
    }

    private String formatearGrado(PersonaGradoAcademico g) {
        // "MAGISTER" → "Magister": en una línea larga de estudios, la mayúscula sostenida grita.
        String grado = g.getGrado().name();
        StringBuilder sb = new StringBuilder(grado.charAt(0) + grado.substring(1).toLowerCase());
        boolean tieneUni = g.getUniversidad() != null && !g.getUniversidad().isBlank();
        if (tieneUni || g.getAnio() != null) {
            sb.append(" — ");
            if (tieneUni) {
                sb.append(g.getUniversidad().trim());
            }
            if (g.getAnio() != null) {
                sb.append(tieneUni ? " (" + g.getAnio() + ")" : String.valueOf(g.getAnio()));
            }
        }
        return sb.toString();
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
                .tipo(s.tipoEfectivo().name())
                // Trayectoria: el doctorando decide a quién confía su tesis.
                .categoria(asesor != null && asesor.getCategoria() != null ? categoriaLabel(asesor.getCategoria().name()) : null)
                .condicion(asesor != null && asesor.getCondicion() != null ? condicionLabel(asesor.getCondicion().name()) : null)
                .cargoActual(cargoActual(s.getAsesorDocenteId()))
                .centroLaboral(centroLaboral(s.getAsesorDocenteId(), false))
                .centroLaboralDetalle(centroLaboral(s.getAsesorDocenteId(), true))
                .experienciaAnios(experienciaAnios(s.getAsesorDocenteId()))
                .orcid(p != null ? p.getOrcid() : null)
                .estudios(estudios(s.getAsesorDocenteId()))
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
