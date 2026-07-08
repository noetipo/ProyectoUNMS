package unmsm.edu.pe.tesis.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.CrearSolicitudRequest;
import unmsm.edu.pe.tesis.application.dto.ResponderSolicitudRequest;
import unmsm.edu.pe.tesis.application.dto.SolicitudResponse;
import unmsm.edu.pe.tesis.application.mapper.SolicitudAsesoriaMapper;
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.enums.DecisionSolicitud;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;
import unmsm.edu.pe.tesis.domain.repositories.SolicitudAsesoriaRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudAsesoriaServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock PersonaRepository personaRepository;
    @Mock EstudianteRepository estudianteRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock LineaInvestigacionRepository lineaInvestigacionRepository;
    @Mock DocenteLineaInvestigacionRepository docenteLineaRepository;
    @Mock SolicitudAsesoriaRepository solicitudRepository;
    @Mock unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository tesisAutorRepository;
    @Mock unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository asesoriaRepository;

    @Mock unmsm.edu.pe.tesis.domain.services.ResolverDatosPlantillaService resolverPlantilla;
    @Spy com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    @Spy SolicitudAsesoriaMapper mapper = new SolicitudAsesoriaMapper();

    @InjectMocks SolicitudAsesoriaServiceImpl service;

    private UUID userId;
    private UUID personaId;
    private UUID docenteId;
    private UUID lineaId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        personaId = UUID.randomUUID();
        docenteId = UUID.randomUUID();
        lineaId = UUID.randomUUID();
        // El snapshot de documentos se resuelve al crear/aceptar; en unit test devuelve un mapa vacío.
        lenient().when(resolverPlantilla.resolverSolicitud(any(), any())).thenReturn(new java.util.LinkedHashMap<>());
        lenient().when(resolverPlantilla.resolverCarta(any(), any())).thenReturn(new java.util.LinkedHashMap<>());
    }

    private void stubEstudianteAutenticado() {
        when(securityUtils.getCurrentUserIdAsUUID()).thenReturn(userId);
        when(personaRepository.findByUserId(userId)).thenReturn(Optional.of(Persona.builder().id(personaId).build()));
        when(estudianteRepository.findByPersonaId(personaId))
                .thenReturn(Optional.of(Estudiante.builder().personaId(personaId).build()));
    }

    private void stubDocenteAutenticado(UUID docPersonaId) {
        when(securityUtils.getCurrentUserIdAsUUID()).thenReturn(userId);
        when(personaRepository.findByUserId(userId)).thenReturn(Optional.of(Persona.builder().id(docPersonaId).build()));
        when(docenteRepository.findByPersonaId(docPersonaId))
                .thenReturn(Optional.of(Docente.builder().personaId(docPersonaId).build()));
    }

    private CrearSolicitudRequest crearRequest() {
        CrearSolicitudRequest r = new CrearSolicitudRequest();
        r.setDocenteId(docenteId);
        r.setLineaInvestigacionId(lineaId);
        r.setTituloTentativo("Título tentativo");
        r.setMensaje("Estimado docente...");
        return r;
    }

    // ── crear ──

    @Test
    void crear_feliz_registraPendiente() {
        stubEstudianteAutenticado();
        when(docenteRepository.findByPersonaId(docenteId))
                .thenReturn(Optional.of(Docente.builder().personaId(docenteId).build()));
        when(lineaInvestigacionRepository.buscarPorId(lineaId))
                .thenReturn(Optional.of(LineaInvestigacion.builder().id(lineaId).nombre("IA").build()));
        when(docenteLineaRepository.existsByDocenteAndLinea(docenteId, lineaId)).thenReturn(true);
        when(solicitudRepository.existePendientePorEstudiante(personaId)).thenReturn(false);
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SolicitudResponse res = service.crear(crearRequest());

        assertEquals("PENDIENTE", res.getEstado());
        assertEquals(docenteId, res.getDocenteId());
        assertEquals(personaId, res.getEstudianteId());
        verify(solicitudRepository).save(any(SolicitudAsesoria.class));
    }

    @Test
    void crear_docenteSinEsaLinea_lanzaBusiness() {
        stubEstudianteAutenticado();
        when(docenteRepository.findByPersonaId(docenteId))
                .thenReturn(Optional.of(Docente.builder().personaId(docenteId).build()));
        when(lineaInvestigacionRepository.buscarPorId(lineaId))
                .thenReturn(Optional.of(LineaInvestigacion.builder().id(lineaId).nombre("IA").build()));
        when(docenteLineaRepository.existsByDocenteAndLinea(docenteId, lineaId)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.crear(crearRequest()));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_yaTienePendiente_lanzaBusiness() {
        stubEstudianteAutenticado();
        when(docenteRepository.findByPersonaId(docenteId))
                .thenReturn(Optional.of(Docente.builder().personaId(docenteId).build()));
        when(lineaInvestigacionRepository.buscarPorId(lineaId))
                .thenReturn(Optional.of(LineaInvestigacion.builder().id(lineaId).nombre("IA").build()));
        when(docenteLineaRepository.existsByDocenteAndLinea(docenteId, lineaId)).thenReturn(true);
        when(solicitudRepository.existePendientePorEstudiante(personaId)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.crear(crearRequest()));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_sinPerfilEstudiante_lanzaBusiness() {
        when(securityUtils.getCurrentUserIdAsUUID()).thenReturn(userId);
        when(personaRepository.findByUserId(userId)).thenReturn(Optional.of(Persona.builder().id(personaId).build()));
        when(estudianteRepository.findByPersonaId(personaId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.crear(crearRequest()));
    }

    // ── responder ──

    private SolicitudAsesoria solicitudPendiente(UUID docPersonaId, UUID estPersonaId) {
        return SolicitudAsesoria.builder()
                .id(UUID.randomUUID())
                .docente(Docente.builder().personaId(docPersonaId).build())
                .estudiante(Estudiante.builder().personaId(estPersonaId).build())
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
    }

    @Test
    void responder_aceptar_quedaAceptada() {
        UUID docP = UUID.randomUUID();
        stubDocenteAutenticado(docP);
        SolicitudAsesoria s = solicitudPendiente(docP, UUID.randomUUID());
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SolicitudResponse res = service.responder(s.getId(),
                new ResponderSolicitudRequest(DecisionSolicitud.ACEPTAR, null));

        assertEquals("ACEPTADA", res.getEstado());
        assertNotNull(res.getFechaRespuesta());
        assertEquals(EstadoSolicitud.ACEPTADA, s.getEstado());
    }

    @Test
    void responder_rechazar_sinMotivo_lanzaValidation() {
        UUID docP = UUID.randomUUID();
        stubDocenteAutenticado(docP);
        SolicitudAsesoria s = solicitudPendiente(docP, UUID.randomUUID());
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));

        assertThrows(ValidationException.class, () -> service.responder(s.getId(),
                new ResponderSolicitudRequest(DecisionSolicitud.RECHAZAR, "  ")));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void responder_rechazar_conMotivo_quedaRechazada() {
        UUID docP = UUID.randomUUID();
        stubDocenteAutenticado(docP);
        SolicitudAsesoria s = solicitudPendiente(docP, UUID.randomUUID());
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SolicitudResponse res = service.responder(s.getId(),
                new ResponderSolicitudRequest(DecisionSolicitud.RECHAZAR, "No hay cupo"));

        assertEquals("RECHAZADA", res.getEstado());
        assertEquals("No hay cupo", res.getMotivoRespuesta());
    }

    @Test
    void responder_solicitudDeOtroDocente_lanzaBusiness() {
        stubDocenteAutenticado(UUID.randomUUID()); // docente autenticado distinto
        SolicitudAsesoria s = solicitudPendiente(UUID.randomUUID(), UUID.randomUUID());
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));

        assertThrows(BusinessException.class, () -> service.responder(s.getId(),
                new ResponderSolicitudRequest(DecisionSolicitud.ACEPTAR, null)));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void responder_noPendiente_lanzaBusiness() {
        UUID docP = UUID.randomUUID();
        stubDocenteAutenticado(docP);
        SolicitudAsesoria s = solicitudPendiente(docP, UUID.randomUUID());
        s.setEstado(EstadoSolicitud.ACEPTADA);
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));

        assertThrows(BusinessException.class, () -> service.responder(s.getId(),
                new ResponderSolicitudRequest(DecisionSolicitud.ACEPTAR, null)));
    }

    @Test
    void responder_solicitudInexistente_lanzaNotFound() {
        stubDocenteAutenticado(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        when(solicitudRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.responder(id,
                new ResponderSolicitudRequest(DecisionSolicitud.ACEPTAR, null)));
    }

    // ── cancelar ──

    @Test
    void cancelar_propiaPendiente_quedaCancelada() {
        stubEstudianteAutenticado();
        SolicitudAsesoria s = solicitudPendiente(UUID.randomUUID(), personaId);
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SolicitudResponse res = service.cancelar(s.getId());

        assertEquals("CANCELADA", res.getEstado());
    }

    @Test
    void cancelar_ajena_lanzaBusiness() {
        stubEstudianteAutenticado();
        SolicitudAsesoria s = solicitudPendiente(UUID.randomUUID(), UUID.randomUUID());
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));

        assertThrows(BusinessException.class, () -> service.cancelar(s.getId()));
        verify(solicitudRepository, never()).save(any());
    }
}
