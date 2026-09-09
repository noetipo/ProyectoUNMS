package unmsm.edu.pe.tesis.infrastructure.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.shared.utils.EmailService;
import unmsm.edu.pe.tesis.application.dto.NotificacionItem;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.repositories.CorreoNotificacionEnviadoRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;
import unmsm.edu.pe.tesis.domain.services.NotificacionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Correo por notificación del alumno: recorre cada tesis, recalcula las notificaciones derivadas
 * (mismas que la campanita) y manda un correo por cada una que no se haya enviado antes (huella
 * = id + descripción), sin depender de una tabla de eventos.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionEmailSchedulerTest {

    @Mock TesisRepository tesisRepository;
    @Mock TesisAutorRepository tesisAutorRepository;
    @Mock PersonaRepository personaRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock NotificacionService notificacionService;
    @Mock CorreoNotificacionEnviadoRepository enviadoRepository;
    @Mock EmailService emailService;

    @InjectMocks NotificacionEmailScheduler scheduler;

    private final UUID TESIS_ID = UUID.randomUUID();
    private final UUID PERSONA_ID = UUID.randomUUID();

    private Tesis tesis;
    private Persona persona;

    @BeforeEach
    void setUp() throws Exception {
        setField("habilitado", true);
        setField("overrideEmail", Optional.empty());
        setField("frontendUrl", "http://localhost:4200");

        tesis = new Tesis();
        tesis.setId(TESIS_ID);

        User user = new User();
        user.setEmail("estudiante@unmsm.edu.pe");
        persona = Persona.builder().id(PERSONA_ID).nombres("Ana").apellidoPaterno("Reyes").user(user).build();

        lenient().when(tesisRepository.listarTodas()).thenReturn(List.of(tesis));
        lenient().when(tesisAutorRepository.estudianteDeTesis(TESIS_ID)).thenReturn(PERSONA_ID);
        lenient().when(personaRepository.buscarPorId(PERSONA_ID)).thenReturn(Optional.of(persona));
    }

    private void setField(String name, Object value) throws Exception {
        var f = NotificacionEmailScheduler.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(scheduler, value);
    }

    private NotificacionItem item(String id, String desc) {
        return NotificacionItem.builder().id(id).title("Título " + id).description(desc)
                .link("/admin/mi-tesis/ejecucion").icon("info").build();
    }

    @Test
    void deshabilitado_noEnviaNiConsulta() {
        setFieldUnchecked("habilitado", false);
        scheduler.enviarCorreosPendientes();
        verifyNoInteractions(tesisRepository, emailService, enviadoRepository);
    }

    @Test
    void notificacionNueva_enviaYRegistraHuella() {
        when(notificacionService.notificacionesDeEstudiante(persona))
                .thenReturn(List.of(item("informe-1", "Tu asesor aprobó el informe final.")));
        when(enviadoRepository.existe(PERSONA_ID, "informe-1|Tu asesor aprobó el informe final.")).thenReturn(false);

        scheduler.enviarCorreosPendientes();

        verify(emailService).sendHtmlEmail(eq("estudiante@unmsm.edu.pe"), eq("Título informe-1"), anyString());
        verify(enviadoRepository).registrar(PERSONA_ID, "informe-1|Tu asesor aprobó el informe final.");
    }

    @Test
    void notificacionYaEnviada_noReenvia() {
        when(notificacionService.notificacionesDeEstudiante(persona))
                .thenReturn(List.of(item("informe-1", "Tu asesor aprobó el informe final.")));
        when(enviadoRepository.existe(PERSONA_ID, "informe-1|Tu asesor aprobó el informe final.")).thenReturn(true);

        scheduler.enviarCorreosPendientes();

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
        verify(enviadoRepository, never()).registrar(any(), any());
    }

    @Test
    void mismoIdConDescripcionNueva_seConsideraNotificacionDistinta() {
        // Avanzó de etapa: mismo id de notificación, texto distinto → la huella cambia y se reenvía
        // (la huella vieja "defensa-1|Tu defensa fue programada." nunca se consulta: ya no aparece
        // entre las notificaciones actuales del alumno).
        when(notificacionService.notificacionesDeEstudiante(persona))
                .thenReturn(List.of(item("defensa-1", "Tu defensa fue reprogramada.")));
        when(enviadoRepository.existe(PERSONA_ID, "defensa-1|Tu defensa fue reprogramada.")).thenReturn(false);

        scheduler.enviarCorreosPendientes();

        verify(emailService).sendHtmlEmail(eq("estudiante@unmsm.edu.pe"), anyString(), anyString());
        verify(enviadoRepository).registrar(PERSONA_ID, "defensa-1|Tu defensa fue reprogramada.");
    }

    @Test
    void conOverride_todosLosCorreosVanAlOverride() throws Exception {
        setField("overrideEmail", Optional.of("leydymayumy@gmail.com"));
        when(notificacionService.notificacionesDeEstudiante(persona))
                .thenReturn(List.of(item("informe-1", "Tu asesor aprobó el informe final.")));
        when(enviadoRepository.existe(any(), anyString())).thenReturn(false);

        scheduler.enviarCorreosPendientes();

        verify(emailService).sendHtmlEmail(eq("leydymayumy@gmail.com"), anyString(), anyString());
    }

    @Test
    void sinEmailPersonal_usaElDelUsuario() {
        when(notificacionService.notificacionesDeEstudiante(persona))
                .thenReturn(List.of(item("informe-1", "Tu asesor aprobó el informe final.")));
        when(enviadoRepository.existe(any(), anyString())).thenReturn(false);

        scheduler.enviarCorreosPendientes();

        verify(emailService).sendHtmlEmail(eq("estudiante@unmsm.edu.pe"), anyString(), anyString());
    }

    @Test
    void emailPersonalPresente_tienePrioridad() {
        persona.setEmailPersonal("personal@gmail.com");
        when(notificacionService.notificacionesDeEstudiante(persona))
                .thenReturn(List.of(item("informe-1", "Tu asesor aprobó el informe final.")));
        when(enviadoRepository.existe(any(), anyString())).thenReturn(false);

        scheduler.enviarCorreosPendientes();

        verify(emailService).sendHtmlEmail(eq("personal@gmail.com"), anyString(), anyString());
    }

    @Test
    void fallaElEnvio_noRegistraHuellaYSigueConLasDemas() {
        when(notificacionService.notificacionesDeEstudiante(persona)).thenReturn(List.of(
                item("informe-1", "desc 1"),
                item("informe-2", "desc 2")));
        when(enviadoRepository.existe(any(), anyString())).thenReturn(false);
        doThrow(new RuntimeException("SMTP caído")).when(emailService)
                .sendHtmlEmail(anyString(), eq("Título informe-1"), anyString());

        assertDoesNotThrow(() -> scheduler.enviarCorreosPendientes());

        verify(enviadoRepository, never()).registrar(PERSONA_ID, "informe-1|desc 1");
        verify(enviadoRepository).registrar(PERSONA_ID, "informe-2|desc 2");
    }

    @Test
    void docenteConNotificacionDeAsesor_envia() {
        UUID docenteId = UUID.randomUUID();
        User user = new User(); user.setEmail("docente@unmsm.edu.pe");
        Persona docentePersona = Persona.builder().id(docenteId).nombres("Carlos").apellidoPaterno("Paredes").user(user).build();
        Docente docente = Docente.builder().personaId(docenteId).build();

        when(tesisRepository.listarTodas()).thenReturn(List.of());
        when(docenteRepository.listarTodosActivos()).thenReturn(List.of(docente));
        when(personaRepository.buscarPorId(docenteId)).thenReturn(Optional.of(docentePersona));
        when(notificacionService.notificacionesDeAsesor(docentePersona))
                .thenReturn(List.of(item("nuevorev-1", "Un estudiante envió su proyecto a revisión.")));
        when(notificacionService.notificacionesDeRevisor(docentePersona)).thenReturn(List.of());
        when(enviadoRepository.existe(any(), anyString())).thenReturn(false);

        scheduler.enviarCorreosPendientes();

        verify(emailService).sendHtmlEmail(eq("docente@unmsm.edu.pe"), anyString(), anyString());
        verify(enviadoRepository).registrar(docenteId, "nuevorev-1|Un estudiante envió su proyecto a revisión.");
    }

    @Test
    void docenteConNotificacionDeRevisor_envia() {
        UUID docenteId = UUID.randomUUID();
        User user = new User(); user.setEmail("revisor@unmsm.edu.pe");
        Persona docentePersona = Persona.builder().id(docenteId).nombres("Diana").apellidoPaterno("Ponce").user(user).build();
        Docente docente = Docente.builder().personaId(docenteId).build();

        when(tesisRepository.listarTodas()).thenReturn(List.of());
        when(docenteRepository.listarTodosActivos()).thenReturn(List.of(docente));
        when(personaRepository.buscarPorId(docenteId)).thenReturn(Optional.of(docentePersona));
        when(notificacionService.notificacionesDeAsesor(docentePersona)).thenReturn(List.of());
        when(notificacionService.notificacionesDeRevisor(docentePersona))
                .thenReturn(List.of(item("revrubrica-1", "Secretaría subió la rúbrica oficial.")));
        when(enviadoRepository.existe(any(), anyString())).thenReturn(false);

        scheduler.enviarCorreosPendientes();

        verify(emailService).sendHtmlEmail(eq("revisor@unmsm.edu.pe"), anyString(), anyString());
        verify(enviadoRepository).registrar(docenteId, "revrubrica-1|Secretaría subió la rúbrica oficial.");
    }

    @Test
    void unDocenteEsAsesorYRevisorALaVez_envíaAmbas() {
        UUID docenteId = UUID.randomUUID();
        User user = new User(); user.setEmail("docente@unmsm.edu.pe");
        Persona docentePersona = Persona.builder().id(docenteId).nombres("Carlos").apellidoPaterno("Paredes").user(user).build();
        Docente docente = Docente.builder().personaId(docenteId).build();

        when(tesisRepository.listarTodas()).thenReturn(List.of());
        when(docenteRepository.listarTodosActivos()).thenReturn(List.of(docente));
        when(personaRepository.buscarPorId(docenteId)).thenReturn(Optional.of(docentePersona));
        when(notificacionService.notificacionesDeAsesor(docentePersona))
                .thenReturn(List.of(item("corr-1", "El estudiante corrigió 2 ítems.")));
        when(notificacionService.notificacionesDeRevisor(docentePersona))
                .thenReturn(List.of(item("revcorr-1", "El estudiante corrigió 1 ítem observado.")));
        when(enviadoRepository.existe(any(), anyString())).thenReturn(false);

        scheduler.enviarCorreosPendientes();

        verify(emailService, times(2)).sendHtmlEmail(eq("docente@unmsm.edu.pe"), anyString(), anyString());
        verify(enviadoRepository).registrar(docenteId, "corr-1|El estudiante corrigió 2 ítems.");
        verify(enviadoRepository).registrar(docenteId, "revcorr-1|El estudiante corrigió 1 ítem observado.");
    }

    @Test
    void docenteSinNotificaciones_noEnviaNada() {
        UUID docenteId = UUID.randomUUID();
        Docente docente = Docente.builder().personaId(docenteId).build();
        User user = new User(); user.setEmail("docente@unmsm.edu.pe");
        when(tesisRepository.listarTodas()).thenReturn(List.of());
        when(docenteRepository.listarTodosActivos()).thenReturn(List.of(docente));
        when(personaRepository.buscarPorId(docenteId)).thenReturn(Optional.of(
                Persona.builder().id(docenteId).user(user).build()));
        when(notificacionService.notificacionesDeAsesor(any())).thenReturn(List.of());
        when(notificacionService.notificacionesDeRevisor(any())).thenReturn(List.of());

        scheduler.enviarCorreosPendientes();

        verify(emailService, never()).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void sinEstudianteAsociado_noFalla() {
        when(tesisAutorRepository.estudianteDeTesis(TESIS_ID)).thenReturn(null);
        assertDoesNotThrow(() -> scheduler.enviarCorreosPendientes());
        verifyNoInteractions(notificacionService, emailService);
    }

    @Test
    void tesisSinPersonaEncontrada_noFalla() {
        when(personaRepository.buscarPorId(PERSONA_ID)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> scheduler.enviarCorreosPendientes());
        verifyNoInteractions(notificacionService, emailService);
    }

    private void setFieldUnchecked(String name, Object value) {
        try {
            setField(name, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
