package unmsm.edu.pe.tutorias.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.security.domain.entities.Role;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.entities.UserRoleAssignment;
import unmsm.edu.pe.security.domain.repositories.RoleRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;
import unmsm.edu.pe.tutorias.application.dto.AsignarEnBloqueRequest;
import unmsm.edu.pe.tutorias.application.dto.AsignarEnBloqueResponse;
import unmsm.edu.pe.tutorias.application.mapper.TutoriaMapper;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;
import unmsm.edu.pe.shared.exceptions.BusinessException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutoriaServiceImplTest {

    @Mock TutoriaRepository tutoriaRepository;
    @Mock EstudianteRepository estudianteRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock TutoriaMapper mapper;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleAssignmentRepository userRoleRepository;

    @InjectMocks TutoriaServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        Field f = TutoriaServiceImpl.class.getDeclaredField("cupoDefault");
        f.setAccessible(true);
        f.setInt(service, 20);
    }

    private Docente docente(UUID id, Integer cupo) {
        Docente d = Docente.builder().personaId(id).cupoMaximoTutoria(cupo)
                .persona(Persona.builder().id(id).apellidoPaterno("Tutor").nombres("Demo").build())
                .build();
        return d;
    }

    private Estudiante estudiante(UUID id) {
        return Estudiante.builder().personaId(id).build();
    }

    private Tutoria vigenteCon(UUID docenteId) {
        return Tutoria.builder().id(UUID.randomUUID())
                .docente(Docente.builder().personaId(docenteId).build())
                .actual(true).build();
    }

    // ── asignar (individual) ──

    @Test
    void asignar_sinVigente_creaNueva() {
        UUID estId = UUID.randomUUID(), docId = UUID.randomUUID();
        when(tutoriaRepository.findVigenteByEstudiante(estId)).thenReturn(Optional.empty());
        when(tutoriaRepository.countActualesByDocente(docId)).thenReturn(0L);
        when(tutoriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.asignar(estudiante(estId), docente(docId, 20), null);

        verify(tutoriaRepository, never()).cerrarVigentePorEstudiante(any(), any());
        verify(tutoriaRepository).save(argThat(t -> Boolean.TRUE.equals(t.getActual())));
    }

    @Test
    void asignar_mismoTutor_idempotente() {
        UUID estId = UUID.randomUUID(), docId = UUID.randomUUID();
        when(tutoriaRepository.findVigenteByEstudiante(estId)).thenReturn(Optional.of(vigenteCon(docId)));

        service.asignar(estudiante(estId), docente(docId, 20), null);

        verify(tutoriaRepository, never()).save(any());
        verify(tutoriaRepository, never()).cerrarVigentePorEstudiante(any(), any());
    }

    @Test
    void asignar_cambioTutor_cierraVigenteYAbreNueva() {
        UUID estId = UUID.randomUUID(), nuevoDoc = UUID.randomUUID(), viejoDoc = UUID.randomUUID();
        when(tutoriaRepository.findVigenteByEstudiante(estId)).thenReturn(Optional.of(vigenteCon(viejoDoc)));
        when(tutoriaRepository.countActualesByDocente(nuevoDoc)).thenReturn(1L);
        when(tutoriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.asignar(estudiante(estId), docente(nuevoDoc, 20), "Cambio solicitado");

        verify(tutoriaRepository).cerrarVigentePorEstudiante(eq(estId), any());
        verify(tutoriaRepository).save(argThat(t -> Boolean.TRUE.equals(t.getActual())));
    }

    @Test
    void asignar_cupoLleno_lanzaBusiness() {
        UUID estId = UUID.randomUUID(), docId = UUID.randomUUID();
        when(tutoriaRepository.findVigenteByEstudiante(estId)).thenReturn(Optional.empty());
        when(tutoriaRepository.countActualesByDocente(docId)).thenReturn(3L);

        assertThrows(BusinessException.class,
                () -> service.asignar(estudiante(estId), docente(docId, 3), null));
        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void asignar_usaCupoDefault_cuandoDocenteSinCupo() {
        UUID estId = UUID.randomUUID(), docId = UUID.randomUUID();
        when(tutoriaRepository.findVigenteByEstudiante(estId)).thenReturn(Optional.empty());
        when(tutoriaRepository.countActualesByDocente(docId)).thenReturn(20L); // == default 20 → lleno

        assertThrows(BusinessException.class,
                () -> service.asignar(estudiante(estId), docente(docId, null), null));
    }

    // ── asignar en bloque ──

    @Test
    void asignarEnBloque_mixNuevoYReemplazo_ok() {
        UUID docId = UUID.randomUUID(), estA = UUID.randomUUID(), estB = UUID.randomUUID();
        when(docenteRepository.findByPersonaId(docId)).thenReturn(Optional.of(docente(docId, 20)));
        when(tutoriaRepository.countActualesByDocente(docId)).thenReturn(0L);
        when(estudianteRepository.findByPersonaId(estA)).thenReturn(Optional.of(estudiante(estA)));
        when(estudianteRepository.findByPersonaId(estB)).thenReturn(Optional.of(estudiante(estB)));
        when(tutoriaRepository.findVigenteByEstudiante(estA)).thenReturn(Optional.empty());
        when(tutoriaRepository.findVigenteByEstudiante(estB)).thenReturn(Optional.of(vigenteCon(UUID.randomUUID())));
        when(tutoriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AsignarEnBloqueResponse res = service.asignarEnBloque(
                new AsignarEnBloqueRequest(docId, List.of(estA, estB), null));

        assertEquals(2, res.getAsignados());
        assertEquals(1, res.getReemplazos());
        assertEquals(0, res.getSinCambio());
        verify(tutoriaRepository, times(2)).save(any(Tutoria.class));
    }

    @Test
    void asignarEnBloque_docenteSinRolTutor_seHabilitaComoTutor() {
        UUID docId = UUID.randomUUID(), estA = UUID.randomUUID(), userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        Docente d = Docente.builder().personaId(docId).cupoMaximoTutoria(20)
                .persona(Persona.builder().id(docId).apellidoPaterno("Tutor").nombres("Nuevo").user(user).build())
                .build();
        when(docenteRepository.findByPersonaId(docId)).thenReturn(Optional.of(d));
        when(tutoriaRepository.countActualesByDocente(docId)).thenReturn(0L);
        when(estudianteRepository.findByPersonaId(estA)).thenReturn(Optional.of(estudiante(estA)));
        when(tutoriaRepository.findVigenteByEstudiante(estA)).thenReturn(Optional.empty());
        when(tutoriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRoleRepository.findRoleCodesByUserId(userId)).thenReturn(List.of()); // aún no es tutor
        when(roleRepository.findByCode("PROF_TUTOR")).thenReturn(Optional.of(Role.builder().code("PROF_TUTOR").build()));

        service.asignarEnBloque(new AsignarEnBloqueRequest(docId, List.of(estA), null));

        verify(userRoleRepository).save(any(UserRoleAssignment.class)); // quedó habilitado como tutor
    }

    @Test
    void asignarEnBloque_yaTieneEsteTutor_sinCambio() {
        UUID docId = UUID.randomUUID(), estA = UUID.randomUUID();
        when(docenteRepository.findByPersonaId(docId)).thenReturn(Optional.of(docente(docId, 20)));
        when(tutoriaRepository.countActualesByDocente(docId)).thenReturn(1L);
        when(estudianteRepository.findByPersonaId(estA)).thenReturn(Optional.of(estudiante(estA)));
        when(tutoriaRepository.findVigenteByEstudiante(estA)).thenReturn(Optional.of(vigenteCon(docId)));

        AsignarEnBloqueResponse res = service.asignarEnBloque(
                new AsignarEnBloqueRequest(docId, List.of(estA), null));

        assertEquals(0, res.getAsignados());
        assertEquals(1, res.getSinCambio());
        verify(tutoriaRepository, never()).save(any());
    }

    // ── finalizar (quitar) ──

    @Test
    void finalizar_delTutorCorrecto_cierraVigente() {
        UUID docId = UUID.randomUUID(), estId = UUID.randomUUID();
        Tutoria vigente = vigenteCon(docId);
        when(tutoriaRepository.findVigenteByEstudiante(estId)).thenReturn(Optional.of(vigente));
        when(tutoriaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.finalizar(docId, estId, "Ya no continúa");

        assertFalse(vigente.getActual());
        assertNotNull(vigente.getFechaFin());
        verify(tutoriaRepository).save(vigente);
    }

    @Test
    void finalizar_tutoriaDeOtroTutor_lanzaBusiness() {
        UUID estId = UUID.randomUUID();
        when(tutoriaRepository.findVigenteByEstudiante(estId)).thenReturn(Optional.of(vigenteCon(UUID.randomUUID())));

        assertThrows(BusinessException.class, () -> service.finalizar(UUID.randomUUID(), estId, null));
        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void finalizar_sinVigente_lanzaBusiness() {
        UUID estId = UUID.randomUUID();
        when(tutoriaRepository.findVigenteByEstudiante(estId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.finalizar(UUID.randomUUID(), estId, null));
        verify(tutoriaRepository, never()).save(any());
    }

    @Test
    void asignarEnBloque_excedeCupo_lanzaBusinessSinAsignar() {
        UUID docId = UUID.randomUUID(), estA = UUID.randomUUID(), estB = UUID.randomUUID();
        when(docenteRepository.findByPersonaId(docId)).thenReturn(Optional.of(docente(docId, 3)));
        when(tutoriaRepository.countActualesByDocente(docId)).thenReturn(2L); // 2 + 2 nuevos > 3
        when(estudianteRepository.findByPersonaId(estA)).thenReturn(Optional.of(estudiante(estA)));
        when(estudianteRepository.findByPersonaId(estB)).thenReturn(Optional.of(estudiante(estB)));
        when(tutoriaRepository.findVigenteByEstudiante(any())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.asignarEnBloque(
                new AsignarEnBloqueRequest(docId, List.of(estA, estB), null)));
        verify(tutoriaRepository, never()).save(any());
    }
}
