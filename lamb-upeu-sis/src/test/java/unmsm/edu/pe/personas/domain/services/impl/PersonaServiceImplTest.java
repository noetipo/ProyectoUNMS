package unmsm.edu.pe.personas.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.application.dto.AgregarPerfilDocenteRequest;
import unmsm.edu.pe.personas.application.dto.CrearPersonaRequest;
import unmsm.edu.pe.personas.application.dto.PersonaResponse;
import unmsm.edu.pe.personas.application.mapper.PersonaMapper;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.enums.*;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.repositories.ProgramaPosgradoRepository;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.entities.UserRoleAssignment;
import unmsm.edu.pe.security.domain.repositories.RoleRepository;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaServiceImplTest {

    @Mock PersonaRepository personaRepository;
    @Mock EstudianteRepository estudianteRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock ProgramaPosgradoRepository programaRepository;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleAssignmentRepository userRoleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock unmsm.edu.pe.personas.domain.services.GuardarPerfilCompletoService guardarPerfilCompletoService;
    @Mock unmsm.edu.pe.personas.domain.services.GuardarLineasInvestigacionService guardarLineasInvestigacionService;
    @Mock unmsm.edu.pe.personas.domain.services.GuardarGradosAcademicosService guardarGradosAcademicosService;
    @Mock unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository personaGradoRepository;
    @Mock DocenteLineaInvestigacionRepository docenteLineaRepository;
    @Mock unmsm.edu.pe.tutorias.domain.services.TutoriaService tutoriaService;

    @Spy PersonaMapper mapper = new PersonaMapper();

    @InjectMocks PersonaServiceImpl service;

    private UUID programaId;

    @BeforeEach
    void setUp() {
        programaId = UUID.randomUUID();
        lenient().when(personaGradoRepository.findByPersonaId(any())).thenReturn(java.util.List.of());
    }

    // ── helpers ──
    private CrearPersonaRequest.PersonaDatos personaDatos() {
        return new CrearPersonaRequest.PersonaDatos(
                TipoDocumento.DNI, "12345678", "Perez", "Lopez", "Juan",
                Sexo.HOMBRE, null, null, "Peruana", Procedencia.NACIONAL,
                "juan@gmail.com", "+51999", false, null);
    }

    private CrearPersonaRequest.CuentaDatos cuentaDatos() {
        return new CrearPersonaRequest.CuentaDatos("jperez", "jperez@unmsm.edu.pe", "secret12");
    }

    private CrearPersonaRequest.EstudianteDatos estudianteDatos() {
        return new CrearPersonaRequest.EstudianteDatos(
                "E001", "M001", "e001@unmsm.edu.pe", 2024, programaId,
                CondicionEstudiante.REGULAR, Financiamiento.AUTOFINANCIADO, null, null);
    }

    private CrearPersonaRequest.DocenteDatos docenteDatos() {
        return new CrearPersonaRequest.DocenteDatos(
                "D001", "d001@unmsm.edu.pe",
                CategoriaDocente.PRINCIPAL, CondicionDocente.NOMBRADO, null);
    }

    private void stubCreacionOk() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.saveUser(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(personaRepository.save(any(Persona.class))).thenAnswer(i -> {
            Persona p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
    }

    private Role role(String code, String name) {
        Role r = Role.builder().code(code).name(name).status(true).build();
        r.setId(UUID.randomUUID());
        return r;
    }

    // ── registrar ──

    @Test
    void registrar_conEstudianteYDocente_creaTodoYAsignaDosRoles() {
        CrearPersonaRequest req = new CrearPersonaRequest(
                personaDatos(), cuentaDatos(), estudianteDatos(), docenteDatos(), null, null);

        when(personaRepository.existsByNumeroDocumento("12345678")).thenReturn(false);
        when(userRepository.existsByUsername("jperez")).thenReturn(false);
        when(userRepository.existsByEmail("jperez@unmsm.edu.pe")).thenReturn(false);
        when(estudianteRepository.existsByCodigoSistema("E001")).thenReturn(false);
        when(estudianteRepository.existsByCodMatricula("M001")).thenReturn(false);
        when(estudianteRepository.existsByEmailInstitucional("e001@unmsm.edu.pe")).thenReturn(false);
        when(docenteRepository.existsByCodigoSistema("D001")).thenReturn(false);
        when(docenteRepository.existsByEmailInstitucional("d001@unmsm.edu.pe")).thenReturn(false);
        when(programaRepository.buscarPorId(programaId))
                .thenReturn(Optional.of(ProgramaPosgrado.builder().nombre("DOCTORADO EN MEDICINA").nivel(NivelPrograma.DOCTORADO).build()));
        when(roleRepository.findByCode("ESTUDIANTE")).thenReturn(Optional.of(role("ESTUDIANTE", "Estudiante")));
        when(roleRepository.findByCode("DOCENTE")).thenReturn(Optional.of(role("DOCENTE", "Docente")));
        when(estudianteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(docenteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        stubCreacionOk();

        PersonaResponse res = service.registrar(req, java.util.Map.of());

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("jperez", res.getUsername());
        assertTrue(res.getPerfiles().contains("ESTUDIANTE"));
        assertTrue(res.getPerfiles().contains("DOCENTE"));
        assertEquals(2, res.getRoles().size());
        verify(userRepository).saveUser(any(User.class));
        verify(personaRepository).save(any(Persona.class));
        verify(estudianteRepository).save(any());
        verify(docenteRepository).save(any());
        verify(userRoleRepository, times(2)).save(any(UserRoleAssignment.class));
        verify(passwordEncoder).encode("secret12");
    }

    @Test
    void registrar_soloEstudiante_asignaUnRol() {
        CrearPersonaRequest req = new CrearPersonaRequest(
                personaDatos(), cuentaDatos(), estudianteDatos(), null, null, null);

        when(personaRepository.existsByNumeroDocumento(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(estudianteRepository.existsByCodigoSistema(anyString())).thenReturn(false);
        when(estudianteRepository.existsByCodMatricula(anyString())).thenReturn(false);
        when(estudianteRepository.existsByEmailInstitucional(anyString())).thenReturn(false);
        when(programaRepository.buscarPorId(programaId))
                .thenReturn(Optional.of(ProgramaPosgrado.builder().nombre("X").nivel(NivelPrograma.MAESTRIA).build()));
        when(roleRepository.findByCode("ESTUDIANTE")).thenReturn(Optional.of(role("ESTUDIANTE", "Estudiante")));
        when(estudianteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        stubCreacionOk();

        PersonaResponse res = service.registrar(req, java.util.Map.of());

        assertEquals(List.of("ESTUDIANTE"), res.getPerfiles());
        verify(docenteRepository, never()).save(any());
        verify(userRoleRepository, times(1)).save(any(UserRoleAssignment.class));
    }

    @Test
    void registrar_sinPerfil_lanzaValidationException() {
        CrearPersonaRequest req = new CrearPersonaRequest(personaDatos(), cuentaDatos(), null, null, null, null);

        assertThrows(ValidationException.class, () -> service.registrar(req, java.util.Map.of()));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    void registrar_documentoDuplicado_lanzaBusinessException() {
        CrearPersonaRequest req = new CrearPersonaRequest(
                personaDatos(), cuentaDatos(), estudianteDatos(), null, null, null);
        when(personaRepository.existsByNumeroDocumento("12345678")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.registrar(req, java.util.Map.of()));
        assertTrue(ex.getMessage().contains("12345678"));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    void registrar_usernameDuplicado_lanzaBusinessException() {
        CrearPersonaRequest req = new CrearPersonaRequest(
                personaDatos(), cuentaDatos(), estudianteDatos(), null, null, null);
        when(personaRepository.existsByNumeroDocumento(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("jperez")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.registrar(req, java.util.Map.of()));
        assertTrue(ex.getMessage().contains("jperez"));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    void registrar_emailDuplicado_lanzaBusinessException() {
        CrearPersonaRequest req = new CrearPersonaRequest(
                personaDatos(), cuentaDatos(), estudianteDatos(), null, null, null);
        when(personaRepository.existsByNumeroDocumento(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("jperez@unmsm.edu.pe")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.registrar(req, java.util.Map.of()));
        verify(userRepository, never()).saveUser(any());
    }

    // ── agregarPerfilDocente ──

    @Test
    void agregarPerfilDocente_feliz_creaDocenteYAsignaRol() {
        UUID personaId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("jperez");
        Persona persona = Persona.builder().id(personaId).nombres("Juan")
                .apellidoPaterno("Perez").numeroDocumento("12345678").user(user).build();

        AgregarPerfilDocenteRequest req = new AgregarPerfilDocenteRequest(
                "D009", "d009@unmsm.edu.pe",
                CategoriaDocente.AUXILIAR, CondicionDocente.CONTRATADO);

        when(personaRepository.buscarPorId(personaId)).thenReturn(Optional.of(persona));
        when(docenteRepository.existsByPersonaId(personaId)).thenReturn(false);
        when(docenteRepository.existsByCodigoSistema("D009")).thenReturn(false);
        when(docenteRepository.existsByEmailInstitucional("d009@unmsm.edu.pe")).thenReturn(false);
        when(roleRepository.findByCode("DOCENTE")).thenReturn(Optional.of(role("DOCENTE", "Docente")));
        // obtener(...) tras crear
        when(estudianteRepository.findByPersonaId(personaId)).thenReturn(Optional.empty());
        when(docenteRepository.findByPersonaId(personaId))
                .thenReturn(Optional.of(Docente.builder().persona(persona).build()));
        when(userRoleRepository.findRoleNamesByUserId(user.getId())).thenReturn(List.of("Docente"));

        PersonaResponse res = service.agregarPerfilDocente(personaId, req);

        assertTrue(res.getPerfiles().contains("DOCENTE"));
        verify(docenteRepository).save(any(Docente.class));
        verify(userRoleRepository).save(any(UserRoleAssignment.class));
    }

    @Test
    void agregarPerfilDocente_yaTienePerfil_lanzaBusinessException() {
        UUID personaId = UUID.randomUUID();
        Persona persona = Persona.builder().id(personaId).build();
        AgregarPerfilDocenteRequest req = new AgregarPerfilDocenteRequest(
                null, null, null, null);

        when(personaRepository.buscarPorId(personaId)).thenReturn(Optional.of(persona));
        when(docenteRepository.existsByPersonaId(personaId)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.agregarPerfilDocente(personaId, req));
        verify(docenteRepository, never()).save(any());
    }

    @Test
    void agregarPerfilDocente_personaNoExiste_lanzaNotFoundException() {
        UUID personaId = UUID.randomUUID();
        AgregarPerfilDocenteRequest req = new AgregarPerfilDocenteRequest(
                null, null, null, null);
        when(personaRepository.buscarPorId(personaId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.agregarPerfilDocente(personaId, req));
        verify(docenteRepository, never()).save(any());
    }
}
