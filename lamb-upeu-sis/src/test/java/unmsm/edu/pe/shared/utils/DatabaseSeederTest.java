package unmsm.edu.pe.shared.utils;

import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.domain.entities.Cargo;
import unmsm.edu.pe.personas.domain.entities.CentroLaboral;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.repositories.CargoRepository;
import unmsm.edu.pe.personas.domain.repositories.CentroLaboralRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.repositories.ProgramaPosgradoRepository;
import unmsm.edu.pe.tesis.domain.repositories.SolicitudAsesoriaRepository;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;
import unmsm.edu.pe.security.domain.entities.*;
import unmsm.edu.pe.security.domain.entities.Module;
import unmsm.edu.pe.security.domain.repositories.*;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock UserRepository userRepository;
    @Mock ParentModuleRepository parentModuleRepository;
    @Mock ModuleRepository moduleRepository;
    @Mock RoleRepository roleRepository;
    @Mock RoleModuleRepository roleModuleRepository;
    @Mock UserRoleAssignmentRepository userRoleRepository;
    @Mock ProgramaPosgradoRepository programaPosgradoRepository;
    @Mock CargoRepository cargoRepository;
    @Mock CentroLaboralRepository centroLaboralRepository;
    @Mock LineaInvestigacionRepository lineaInvestigacionRepository;
    @Mock DocenteLineaInvestigacionRepository docenteLineaInvestigacionRepository;
    @Mock unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository personaGradoAcademicoRepository;
    @Mock unmsm.edu.pe.configuracion.domain.repositories.FacultadRepository facultadRepository;
    @Mock unmsm.edu.pe.configuracion.domain.repositories.LemaAnualRepository lemaAnualRepository;
    @Mock unmsm.edu.pe.configuracion.domain.repositories.ParametroSistemaRepository parametroSistemaRepository;
    @Mock SolicitudAsesoriaRepository solicitudAsesoriaRepository;
    @Mock TutoriaRepository tutoriaRepository;
    @Mock PersonaRepository personaRepository;
    @Mock EstudianteRepository estudianteRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock unmsm.edu.pe.tesis.domain.repositories.TesisRepository tesisRepository;
    @Mock unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository tesisAutorRepository;
    @Mock unmsm.edu.pe.tesis.domain.repositories.SugerenciaAsesorRepository sugerenciaAsesorRepository;
    @Mock jakarta.persistence.EntityManager entityManager;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks DatabaseSeeder seeder;

    private final StartupEvent event = new StartupEvent();
    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@unmsm.edu.pe");
    }

    private void mockAllNew() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        lenient().when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        lenient().when(parentModuleRepository.findByCode(anyString())).thenReturn(Optional.empty());
        lenient().when(moduleRepository.findByCode(anyString())).thenReturn(Optional.empty());
        lenient().when(roleRepository.findByCode(anyString())).thenReturn(Optional.empty());
        lenient().when(roleModuleRepository.findByModuleInAndRole(any(), any())).thenReturn(Collections.emptyList());
        lenient().when(userRoleRepository.findByUser(any())).thenReturn(Collections.emptyList());
        lenient().when(programaPosgradoRepository.existsByNombre(anyString())).thenReturn(false);
        lenient().when(cargoRepository.existsByNombre(anyString())).thenReturn(false);
        lenient().when(centroLaboralRepository.existsByNombre(anyString())).thenReturn(false);
        lenient().when(lineaInvestigacionRepository.existsByNombre(anyString())).thenReturn(false);
        lenient().when(facultadRepository.existsByNombre(anyString())).thenReturn(false);
        lenient().when(parametroSistemaRepository.existsByClave(anyString())).thenReturn(false);
        lenient().when(lemaAnualRepository.existeActivoPorAnio(anyInt())).thenReturn(false);
        // seedUsuariosPrueba consulta programas y líneas (listas vacías por defecto).
        lenient().when(programaPosgradoRepository.getAll()).thenReturn(Collections.emptyList());
        lenient().when(lineaInvestigacionRepository.listar(any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());
    }

    private void setDevUsers(boolean value) throws Exception {
        java.lang.reflect.Field f = DatabaseSeeder.class.getDeclaredField("devUsersEnabled");
        f.setAccessible(true);
        f.set(seeder, value);
    }

    @Nested
    class FullFlow {

        @Test
        void onStart_primerEjecucion_completaSinExcepcion() {
            mockAllNew();
            assertDoesNotThrow(() -> seeder.onStart(event));
        }

        @Test
        void guardaUsuarioAdmin() {
            mockAllNew();
            seeder.onStart(event);
            verify(userRepository).saveUser(any(User.class));
        }

        @Test
        void guarda4ModulosPadres() {
            mockAllNew();
            seeder.onStart(event);
            verify(parentModuleRepository, times(4)).save(any(ParentModule.class));
        }

        @Test
        void guarda33Modulos() {
            mockAllNew();
            seeder.onStart(event);
            verify(moduleRepository, times(33)).save(any(Module.class));
        }

        @Test
        void guarda16Roles() {
            mockAllNew();
            seeder.onStart(event);
            verify(roleRepository, times(16)).save(any(Role.class));
        }

        @Test
        void creaRolesEstudianteYDocente() {
            mockAllNew();
            List<String> codes = new ArrayList<>();
            doAnswer(inv -> { codes.add(((Role) inv.getArgument(0)).getCode()); return inv.getArgument(0); })
                    .when(roleRepository).save(any(Role.class));

            seeder.onStart(event);

            assertTrue(codes.contains("ESTUDIANTE"));
            assertTrue(codes.contains("DOCENTE"));
            assertTrue(codes.contains("ADMIN"));
        }

        @Test
        void guarda69AsignacionesRoleModule() {
            mockAllNew();
            seeder.onStart(event);
            verify(roleModuleRepository, times(69)).save(any(RoleModule.class));
        }

        @Test
        void asignaRolAdminAlUsuarioAdmin() {
            mockAllNew();
            seeder.onStart(event);
            verify(userRoleRepository).save(any(UserRoleAssignment.class));
        }

        @Test
        void guarda6ProgramasPosgrado() {
            mockAllNew();
            seeder.onStart(event);
            verify(programaPosgradoRepository, times(18)).save(any(ProgramaPosgrado.class));
        }

        @Test
        void guarda15Cargos() {
            mockAllNew();
            seeder.onStart(event);
            verify(cargoRepository, times(15)).save(any(Cargo.class));
        }

        @Test
        void guarda18CentrosLaborales() {
            mockAllNew();
            seeder.onStart(event);
            verify(centroLaboralRepository, times(18)).save(any(CentroLaboral.class));
        }

        @Test
        void guarda10LineasInvestigacion() {
            mockAllNew();
            seeder.onStart(event);
            verify(lineaInvestigacionRepository, times(10)).save(any(LineaInvestigacion.class));
        }

        @Test
        void sinDevUsers_noCreaPersonasDePrueba() {
            mockAllNew(); // devUsersEnabled = false por defecto
            seeder.onStart(event);
            // Sin usuarios de prueba no se crea ninguna persona (el admin no tiene persona).
            verify(personaRepository, never()).save(any(Persona.class));
        }
    }

    @Nested
    class Idempotencia {

        @Test
        void adminUserExiste_noCreaNuevoUsuario() {
            mockAllNew();
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

            seeder.onStart(event);

            verify(userRepository, never()).saveUser(any());
        }

        @Test
        void todosLosProgramasExisten_noCreaNuevos() {
            mockAllNew();
            when(programaPosgradoRepository.existsByNombre(anyString())).thenReturn(true);

            seeder.onStart(event);

            verify(programaPosgradoRepository, never()).save(any());
        }

        @Test
        void todosLosCargosYCentrosExisten_noCreaNuevos() {
            mockAllNew();
            when(cargoRepository.existsByNombre(anyString())).thenReturn(true);
            when(centroLaboralRepository.existsByNombre(anyString())).thenReturn(true);

            seeder.onStart(event);

            verify(cargoRepository, never()).save(any());
            verify(centroLaboralRepository, never()).save(any());
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void repositorioLanzaExcepcion_envuelveEnRuntimeException() {
            when(userRepository.findByUsername(anyString()))
                    .thenThrow(new RuntimeException("DB connection failed"));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> seeder.onStart(event));
            assertEquals("Error fatal en seeding", ex.getMessage());
        }
    }

    @Nested
    class UsuariosDePrueba {

        // 2 de gestión + 10 estudiantes + 10 docentes (usuario = <apellido>.<nombre>).
        private static final java.util.Set<String> DEV_USERNAMES = java.util.Set.of(
                "coordinador", "secretaria",
                "quispe.ana", "mamani.luis", "flores.carmen", "huaman.jose", "condori.rosa",
                "vargas.pedro", "rojas.lucia", "chavez.miguel", "ramos.elena", "torres.jorge",
                "salazar.roberto", "nunez.patricia", "paredes.carlos", "caceres.silvia", "rios.fernando",
                "vega.marta", "ledesma.andres", "bravo.teresa", "cabrera.julio", "ponce.diana");

        @Test
        void devUsersHabilitado_crea22UsuariosDePrueba() throws Exception {
            mockAllNew();
            setDevUsers(true);
            seeder.onStart(event);
            // admin (1) + 22 usuarios de prueba (2 gestión + 10 estudiantes + 10 docentes)
            verify(userRepository, times(23)).saveUser(any(User.class));
            verify(personaRepository, times(22)).save(any(Persona.class));
        }

        @Test
        void devUsersDeshabilitado_noCreaUsuariosDePrueba() {
            mockAllNew(); // devUsersEnabled = false por defecto
            seeder.onStart(event);
            verify(userRepository, times(1)).saveUser(any(User.class)); // solo admin
        }

        @Test
        void devUsers_idempotente_noRecrea() throws Exception {
            mockAllNew();
            setDevUsers(true);
            when(userRepository.findByUsername(argThat(u -> DEV_USERNAMES.contains(u))))
                    .thenReturn(Optional.of(new User()));

            seeder.onStart(event);

            verify(userRepository, times(1)).saveUser(any(User.class)); // solo admin; los de prueba ya existen
            verify(personaRepository, never()).save(any(Persona.class));
        }
    }
}
