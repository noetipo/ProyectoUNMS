package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import unmsm.edu.pe.personas.application.dto.AgregarPerfilDocenteRequest;
import unmsm.edu.pe.personas.application.dto.ArchivoSubido;
import unmsm.edu.pe.personas.application.dto.CrearPersonaRequest;
import unmsm.edu.pe.personas.application.dto.PersonaListItem;
import unmsm.edu.pe.personas.application.dto.PersonaResponse;
import unmsm.edu.pe.personas.application.dto.PersonaUpdateRequest;
import unmsm.edu.pe.personas.application.mapper.PersonaMapper;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.personas.domain.services.GuardarLineasInvestigacionService;
import unmsm.edu.pe.personas.domain.services.GuardarPerfilCompletoService;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.Map;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.repositories.ProgramaPosgradoRepository;
import unmsm.edu.pe.personas.domain.services.PersonaService;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.entities.UserRoleAssignment;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.security.domain.repositories.RoleRepository;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PersonaServiceImpl implements PersonaService {

    private static final Logger LOG = Logger.getLogger(PersonaServiceImpl.class);

    private static final String ROLE_ESTUDIANTE = "ESTUDIANTE";
    private static final String ROLE_DOCENTE = "DOCENTE";

    @Inject PersonaRepository personaRepository;
    @Inject EstudianteRepository estudianteRepository;
    @Inject DocenteRepository docenteRepository;
    @Inject ProgramaPosgradoRepository programaRepository;
    @Inject UserRepository userRepository;
    @Inject RoleRepository roleRepository;
    @Inject UserRoleAssignmentRepository userRoleRepository;
    @Inject PasswordEncoder passwordEncoder;
    @Inject PersonaMapper mapper;
    @Inject GuardarPerfilCompletoService guardarPerfilCompletoService;
    @Inject GuardarLineasInvestigacionService guardarLineasInvestigacionService;
    @Inject unmsm.edu.pe.personas.domain.services.GuardarGradosAcademicosService guardarGradosAcademicosService;
    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository personaGradoRepository;
    @Inject DocenteLineaInvestigacionRepository docenteLineaRepository;
    @Inject unmsm.edu.pe.tutorias.domain.services.TutoriaService tutoriaService;

    @Override
    @Transactional
    public PersonaResponse registrar(CrearPersonaRequest request, Map<TipoDocumento, ArchivoSubido> archivos) {
        CrearPersonaRequest.PersonaDatos pd = request.getPersona();
        CrearPersonaRequest.CuentaDatos cd = request.getCuenta();
        CrearPersonaRequest.EstudianteDatos ed = request.getEstudiante();
        CrearPersonaRequest.DocenteDatos dd = request.getDocente();

        // Al menos un perfil
        if (ed == null && dd == null) {
            throw new ValidationException("Debe indicar al menos un perfil (estudiante o docente)");
        }

        // Unicidad persona / cuenta
        if (personaRepository.existsByNumeroDocumento(pd.getNumeroDocumento())) {
            throw new BusinessException("Ya existe una persona con el número de documento: " + pd.getNumeroDocumento());
        }
        if (pd.getOrcid() != null && !pd.getOrcid().isBlank() && personaRepository.existsByOrcid(pd.getOrcid())) {
            throw new BusinessException("Ya existe una persona con el ORCID: " + pd.getOrcid());
        }
        if (userRepository.existsByUsername(cd.getUsername())) {
            throw new BusinessException("El username ya existe: " + cd.getUsername());
        }
        if (userRepository.existsByEmail(cd.getEmail())) {
            throw new BusinessException("El email ya existe: " + cd.getEmail());
        }

        // Unicidad de perfiles (para responder 409 claro antes de tocar la BD)
        if (ed != null) {
            validarUnicidadEstudiante(ed.getCodigoSistema(), ed.getCodMatricula(), ed.getEmailInstitucional());
        }
        if (dd != null) {
            validarUnicidadDocente(dd.getCodigoSistema(), dd.getEmailInstitucional());
        }

        // Cuenta de acceso
        User user = new User();
        user.setUsername(cd.getUsername());
        user.setEmail(cd.getEmail());
        user.setPassword(passwordEncoder.encode(cd.getPassword()));
        user.setFirstName(pd.getNombres());
        user.setLastName(((pd.getApellidoPaterno() != null ? pd.getApellidoPaterno() : "") + " "
                + (pd.getApellidoMaterno() != null ? pd.getApellidoMaterno() : "")).trim());
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.saveUser(user);

        // Persona
        Persona persona = mapper.toPersona(pd);
        persona.setUser(savedUser);
        Persona savedPersona = personaRepository.save(persona);

        List<String> roles = new ArrayList<>();
        Estudiante estudiante = null;
        Docente docente = null;

        // Perfil estudiante
        if (ed != null) {
            ProgramaPosgrado programa = programaRepository.buscarPorId(ed.getProgramaId())
                    .orElseThrow(() -> new BusinessException("Programa de posgrado no encontrado: " + ed.getProgramaId()));
            estudiante = mapper.toEstudiante(ed, savedPersona, programa);
            estudiante = estudianteRepository.save(estudiante);
            roles.add(asignarRol(savedUser, ROLE_ESTUDIANTE));
            // Tutor académico (opcional): cierra el vigente y abre el nuevo, en la misma transacción.
            if (ed.getTutorId() != null) {
                tutoriaService.asignarIndividual(estudiante.getPersonaId(), ed.getTutorId(), null);
            }
        }

        // Perfil docente
        if (dd != null) {
            docente = mapper.toDocente(dd, savedPersona);
            docente = docenteRepository.save(docente);
            roles.add(asignarRol(savedUser, ROLE_DOCENTE));
            // Líneas de investigación del docente (misma transacción). Solo si hay perfil docente.
            guardarLineasInvestigacionService.aplicar(docente, dd.getLineasInvestigacion());
        }

        // Grados académicos de la persona (lista; misma transacción).
        guardarGradosAcademicosService.aplicar(savedPersona, request.getGradosAcademicos());

        // Historiales (cargos/centros) + documentos en la misma transacción.
        // Al crear exige DNI + PARTIDA_NACIMIENTO; si falla, hace rollback y limpia archivos.
        guardarPerfilCompletoService.aplicar(savedPersona, request.getPerfil(), archivos, true);

        LOG.info("Persona registrada: " + savedPersona.getId() + " perfiles=" + roles);
        return conGrados(conTutor(conLineas(mapper.toResponse(savedPersona, estudiante, docente, roles), docente), estudiante), savedPersona.getId());
    }

    @Override
    @Transactional
    public PersonaResponse agregarPerfilDocente(UUID personaId, AgregarPerfilDocenteRequest request) {
        Persona persona = personaRepository.buscarPorId(personaId)
                .orElseThrow(() -> new NotFoundException("Persona no encontrada: " + personaId));

        if (docenteRepository.existsByPersonaId(personaId)) {
            throw new BusinessException("La persona ya tiene perfil docente");
        }

        validarUnicidadDocente(request.getCodigoSistema(), request.getEmailInstitucional());

        Docente docente = mapper.toDocente(request, persona);
        docenteRepository.save(docente);

        if (persona.getUser() != null) {
            asignarRol(persona.getUser(), ROLE_DOCENTE);
        }

        LOG.info("Perfil docente agregado a persona: " + personaId);
        return obtener(personaId);
    }

    @Override
    public PersonaResponse obtener(UUID personaId) {
        Persona persona = personaRepository.buscarPorId(personaId)
                .orElseThrow(() -> new NotFoundException("Persona no encontrada: " + personaId));

        Estudiante estudiante = estudianteRepository.findByPersonaId(personaId).orElse(null);
        Docente docente = docenteRepository.findByPersonaId(personaId).orElse(null);

        List<String> roles = persona.getUser() != null
                ? userRoleRepository.findRoleNamesByUserId(persona.getUser().getId())
                : List.of();

        return conGrados(conTutor(conLineas(mapper.toResponse(persona, estudiante, docente, roles), docente), estudiante), personaId);
    }

    @Override
    public PageResponse<PersonaListItem> listar(String search, String tipoPerfil, Boolean activo, int page, int size) {
        List<PersonaListItem> content = personaRepository.listar(search, tipoPerfil, activo, page, size).stream()
                .map(mapper::toPersonaListItem)
                .collect(java.util.stream.Collectors.toList());
        long total = personaRepository.contar(search, tipoPerfil, activo);
        return PageResponse.of(content, total, page, size);
    }

    @Override
    @Transactional
    public PersonaResponse actualizar(UUID personaId, PersonaUpdateRequest req) {
        Persona persona = personaRepository.buscarPorId(personaId)
                .orElseThrow(() -> new NotFoundException("Persona no encontrada: " + personaId));

        // Unicidad si cambian documento u orcid
        if (req.getNumeroDocumento() != null && !req.getNumeroDocumento().isBlank()
                && !req.getNumeroDocumento().equals(persona.getNumeroDocumento())
                && personaRepository.existsByNumeroDocumento(req.getNumeroDocumento())) {
            throw new BusinessException("Ya existe una persona con el número de documento: " + req.getNumeroDocumento());
        }
        if (req.getOrcid() != null && !req.getOrcid().isBlank()
                && !req.getOrcid().equals(persona.getOrcid())
                && personaRepository.existsByOrcid(req.getOrcid())) {
            throw new BusinessException("Ya existe una persona con el ORCID: " + req.getOrcid());
        }

        // Datos de persona (solo los provistos)
        if (req.getTipoDocumento() != null) persona.setTipoDocumento(req.getTipoDocumento());
        if (req.getNumeroDocumento() != null) persona.setNumeroDocumento(req.getNumeroDocumento().trim());
        if (req.getApellidoPaterno() != null) persona.setApellidoPaterno(req.getApellidoPaterno());
        if (req.getApellidoMaterno() != null) persona.setApellidoMaterno(req.getApellidoMaterno());
        if (req.getNombres() != null) persona.setNombres(req.getNombres());
        if (req.getSexo() != null) persona.setSexo(req.getSexo());
        if (req.getFechaNacimiento() != null) persona.setFechaNacimiento(req.getFechaNacimiento());
        if (req.getEstadoCivil() != null) persona.setEstadoCivil(req.getEstadoCivil());
        if (req.getNacionalidad() != null) persona.setNacionalidad(req.getNacionalidad());
        if (req.getProcedencia() != null) persona.setProcedencia(req.getProcedencia());
        if (req.getEmailPersonal() != null) persona.setEmailPersonal(req.getEmailPersonal());
        if (req.getCelular() != null) persona.setCelular(req.getCelular());
        if (req.getDiscapacidad() != null) persona.setDiscapacidad(req.getDiscapacidad());
        if (req.getOrcid() != null) persona.setOrcid(req.getOrcid().isBlank() ? null : req.getOrcid().trim());
        personaRepository.save(persona);

        // Perfil estudiante (solo si existe)
        if (req.getEstudiante() != null) {
            estudianteRepository.findByPersonaId(personaId).ifPresent(est -> {
                PersonaUpdateRequest.EstudiantePerfil e = req.getEstudiante();
                if (e.getCodMatricula() != null) est.setCodMatricula(e.getCodMatricula());
                if (e.getEmailInstitucional() != null) est.setEmailInstitucional(e.getEmailInstitucional());
                if (e.getAnioIngreso() != null) est.setAnioIngreso(e.getAnioIngreso());
                if (e.getCondicion() != null) est.setCondicion(e.getCondicion());
                if (e.getFinanciamiento() != null) est.setFinanciamiento(e.getFinanciamiento());
                if (e.getObservaciones() != null) est.setObservaciones(e.getObservaciones());
                if (e.getProgramaId() != null) {
                    ProgramaPosgrado prog = programaRepository.buscarPorId(e.getProgramaId())
                            .orElseThrow(() -> new BusinessException("Programa no encontrado: " + e.getProgramaId()));
                    est.setPrograma(prog);
                }
                estudianteRepository.save(est);
                // Tutor académico (opcional): cambio con historial en la misma transacción.
                if (e.getTutorId() != null) {
                    tutoriaService.asignarIndividual(est.getPersonaId(), e.getTutorId(), null);
                }
            });
        }

        // Perfil docente (solo si existe)
        if (req.getDocente() != null) {
            docenteRepository.findByPersonaId(personaId).ifPresent(doc -> {
                PersonaUpdateRequest.DocentePerfil d = req.getDocente();
                if (d.getEmailInstitucional() != null) doc.setEmailInstitucional(d.getEmailInstitucional());
                if (d.getCategoria() != null) doc.setCategoria(d.getCategoria());
                if (d.getCondicion() != null) doc.setCondicion(d.getCondicion());
                docenteRepository.save(doc);
                // Reemplazo idempotente de líneas de investigación (si se enviaron).
                guardarLineasInvestigacionService.aplicar(doc, d.getLineasInvestigacion());
            });
        }

        // Grados académicos de la persona (lista; reemplazo idempotente si se enviaron).
        guardarGradosAcademicosService.aplicar(persona, req.getGradosAcademicos());

        return obtener(personaId);
    }

    @Override
    @Transactional
    public void eliminarLogico(UUID personaId) {
        Persona persona = personaRepository.buscarPorId(personaId)
                .orElseThrow(() -> new NotFoundException("Persona no encontrada: " + personaId));
        persona.setActive(false);
        personaRepository.save(persona);
        LOG.info("Persona desactivada (borrado lógico): " + personaId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Completa la respuesta con los grados de la persona; si es docente, fija el grado principal. */
    private PersonaResponse conGrados(PersonaResponse resp, UUID personaId) {
        List<unmsm.edu.pe.personas.domain.entities.PersonaGradoAcademico> grados =
                personaGradoRepository.findByPersonaId(personaId);
        resp.setGradosAcademicos(mapper.toGradoItems(grados));
        if (resp.getDocente() != null) {
            grados.stream()
                    .filter(g -> Boolean.TRUE.equals(g.getPrincipal()) && g.getGrado() != null)
                    .findFirst()
                    .ifPresent(g -> resp.getDocente().setGradoAcademico(g.getGrado().name()));
        }
        return resp;
    }

    /** Completa la respuesta con las líneas de investigación del docente (si tiene perfil). */
    private PersonaResponse conLineas(PersonaResponse resp, Docente docente) {
        if (docente == null || resp.getDocente() == null) {
            return resp;
        }
        List<PersonaResponse.LineaInvestigacionItem> items =
                docenteLineaRepository.findByDocenteId(docente.getPersonaId()).stream()
                        .map(dli -> PersonaResponse.LineaInvestigacionItem.builder()
                                .id(dli.getLineaInvestigacion() != null ? dli.getLineaInvestigacion().getId() : null)
                                .nombre(dli.getLineaInvestigacion() != null ? dli.getLineaInvestigacion().getNombre() : null)
                                .esPrincipal(dli.getEsPrincipal())
                                .build())
                        .collect(java.util.stream.Collectors.toList());
        resp.getDocente().setLineasInvestigacion(items);
        return resp;
    }

    /** Completa la respuesta con el tutor vigente del estudiante (si tiene perfil). */
    private PersonaResponse conTutor(PersonaResponse resp, Estudiante estudiante) {
        if (estudiante == null || resp.getEstudiante() == null) {
            return resp;
        }
        unmsm.edu.pe.tutorias.application.dto.TutorVigenteItem v = tutoriaService.tutorVigente(estudiante.getPersonaId());
        if (v != null) {
            resp.getEstudiante().setTutorId(v.getTutorId());
            resp.getEstudiante().setTutorNombre(v.getTutorNombre());
        }
        return resp;
    }

    private String asignarRol(User user, String roleCode) {
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("Rol no configurado en el sistema: " + roleCode));
        UserRoleAssignment assignment = UserRoleAssignment.builder()
                .user(user)
                .role(role)
                .assigned(true)
                .build();
        userRoleRepository.save(assignment);
        return role.getName() != null ? role.getName() : role.getCode();
    }

    private void validarUnicidadEstudiante(String codigoSistema, String codMatricula, String emailInstitucional) {
        if (codigoSistema != null && !codigoSistema.isBlank() && estudianteRepository.existsByCodigoSistema(codigoSistema)) {
            throw new BusinessException("Ya existe un estudiante con el código de sistema: " + codigoSistema);
        }
        if (codMatricula != null && !codMatricula.isBlank() && estudianteRepository.existsByCodMatricula(codMatricula)) {
            throw new BusinessException("Ya existe un estudiante con el código de matrícula: " + codMatricula);
        }
        if (emailInstitucional != null && !emailInstitucional.isBlank()
                && estudianteRepository.existsByEmailInstitucional(emailInstitucional)) {
            throw new BusinessException("Ya existe un estudiante con el email institucional: " + emailInstitucional);
        }
    }

    private void validarUnicidadDocente(String codigoSistema, String emailInstitucional) {
        if (codigoSistema != null && !codigoSistema.isBlank() && docenteRepository.existsByCodigoSistema(codigoSistema)) {
            throw new BusinessException("Ya existe un docente con el código de sistema: " + codigoSistema);
        }
        if (emailInstitucional != null && !emailInstitucional.isBlank()
                && docenteRepository.existsByEmailInstitucional(emailInstitucional)) {
            throw new BusinessException("Ya existe un docente con el email institucional: " + emailInstitucional);
        }
    }
}
