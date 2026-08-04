package unmsm.edu.pe.tesis.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.enums.DecisionSolicitud;
import unmsm.edu.pe.tesis.domain.enums.EstadoSolicitud;
import unmsm.edu.pe.tesis.domain.enums.TipoAsesoria;
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
    @Mock unmsm.edu.pe.tesis.domain.repositories.SugerenciaAsesorRepository sugerenciaRepository;
    @Mock unmsm.edu.pe.tesis.domain.repositories.TesisAutorRepository tesisAutorRepository;
    @Mock unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository asesoriaRepository;
    @Mock AsesorRolService asesorRolService;

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
        when(sugerenciaRepository.existe(personaId, docenteId)).thenReturn(true);
        when(solicitudRepository.existePendientePorEstudiante(personaId)).thenReturn(false);
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SolicitudResponse res = service.crear(crearRequest());

        assertEquals("PENDIENTE", res.getEstado());
        assertEquals(docenteId, res.getDocenteId());
        assertEquals(personaId, res.getEstudianteId());
        verify(solicitudRepository).save(any(SolicitudAsesoria.class));
    }

    // ── regla: 1 asesor + 1 co-asesor (opcional) ──

    /** Deja la solicitud lista para pasar todas las validaciones previas al cupo. */
    private void stubCrearValido() {
        stubEstudianteAutenticado();
        when(docenteRepository.findByPersonaId(docenteId))
                .thenReturn(Optional.of(Docente.builder().personaId(docenteId).build()));
        when(lineaInvestigacionRepository.buscarPorId(lineaId))
                .thenReturn(Optional.of(LineaInvestigacion.builder().id(lineaId).nombre("IA").build()));
        when(docenteLineaRepository.existsByDocenteAndLinea(docenteId, lineaId)).thenReturn(true);
        when(sugerenciaRepository.existe(personaId, docenteId)).thenReturn(true);
        when(solicitudRepository.existePendientePorEstudiante(personaId)).thenReturn(false);
    }

    private Asesoria asesoriaDe(UUID tesisId, UUID docente, String tipo) {
        return Asesoria.builder().tesisId(tesisId).docenteId(docente).tipo(tipo).build();
    }

    @Test
    void crear_comoAsesor_cuandoYaHayAsesor_lanzaBusiness() {
        stubCrearValido();
        UUID tesisId = UUID.randomUUID();
        when(tesisAutorRepository.tesisActivaId(personaId)).thenReturn(tesisId);
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR"))
                .thenReturn(Optional.of(asesoriaDe(tesisId, UUID.randomUUID(), "ASESOR")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.crear(crearRequest()));
        assertTrue(ex.getMessage().contains("co-asesor"));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_comoCoasesor_sinAsesorPrevio_lanzaBusiness() {
        stubCrearValido();
        UUID tesisId = UUID.randomUUID();
        when(tesisAutorRepository.tesisActivaId(personaId)).thenReturn(tesisId);
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")).thenReturn(Optional.empty());

        CrearSolicitudRequest r = crearRequest();
        r.setTipo(TipoAsesoria.COASESOR);

        assertThrows(BusinessException.class, () -> service.crear(r));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_comoCoasesor_cuandoYaHayCoasesor_lanzaBusiness() {
        stubCrearValido();
        UUID tesisId = UUID.randomUUID();
        when(tesisAutorRepository.tesisActivaId(personaId)).thenReturn(tesisId);
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR"))
                .thenReturn(Optional.of(asesoriaDe(tesisId, UUID.randomUUID(), "ASESOR")));
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR"))
                .thenReturn(Optional.of(asesoriaDe(tesisId, UUID.randomUUID(), "COASESOR")));

        CrearSolicitudRequest r = crearRequest();
        r.setTipo(TipoAsesoria.COASESOR);

        assertThrows(BusinessException.class, () -> service.crear(r));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_comoCoasesor_conElMismoDocenteQueYaEsAsesor_lanzaBusiness() {
        stubCrearValido();
        UUID tesisId = UUID.randomUUID();
        when(tesisAutorRepository.tesisActivaId(personaId)).thenReturn(tesisId);
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR"))
                .thenReturn(Optional.of(asesoriaDe(tesisId, docenteId, "ASESOR")));
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR")).thenReturn(Optional.empty());

        CrearSolicitudRequest r = crearRequest();
        r.setTipo(TipoAsesoria.COASESOR);

        assertThrows(BusinessException.class, () -> service.crear(r));
    }

    @Test
    void crear_comoCoasesor_conAsesorYCupoLibre_registraPendiente() {
        stubCrearValido();
        UUID tesisId = UUID.randomUUID();
        when(tesisAutorRepository.tesisActivaId(personaId)).thenReturn(tesisId);
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR"))
                .thenReturn(Optional.of(asesoriaDe(tesisId, UUID.randomUUID(), "ASESOR")));
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR")).thenReturn(Optional.empty());
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CrearSolicitudRequest r = crearRequest();
        r.setTipo(TipoAsesoria.COASESOR);

        assertEquals("PENDIENTE", service.crear(r).getEstado());
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
        when(sugerenciaRepository.existe(personaId, docenteId)).thenReturn(true);
        when(solicitudRepository.existePendientePorEstudiante(personaId)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.crear(crearRequest()));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void crear_asesorNoSugeridoPorTutor_lanzaBusiness() {
        stubEstudianteAutenticado();
        when(docenteRepository.findByPersonaId(docenteId))
                .thenReturn(Optional.of(Docente.builder().personaId(docenteId).build()));
        when(lineaInvestigacionRepository.buscarPorId(lineaId))
                .thenReturn(Optional.of(LineaInvestigacion.builder().id(lineaId).nombre("IA").build()));
        when(docenteLineaRepository.existsByDocenteAndLinea(docenteId, lineaId)).thenReturn(true);
        when(sugerenciaRepository.existe(personaId, docenteId)).thenReturn(false); // el tutor no lo sugirió

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
    void responder_aceptarCoasesoria_creaAsesoriaCOASESOR_yOtorgaRol() {
        UUID docP = UUID.randomUUID();
        UUID estP = UUID.randomUUID();
        UUID tesisId = UUID.randomUUID();
        stubDocenteAutenticado(docP);

        SolicitudAsesoria s = solicitudPendiente(docP, estP);
        s.setTipo(TipoAsesoria.COASESOR);
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(tesisAutorRepository.tesisActivaId(estP)).thenReturn(tesisId);
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR"))
                .thenReturn(Optional.of(asesoriaDe(tesisId, UUID.randomUUID(), "ASESOR")));
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR")).thenReturn(Optional.empty());

        service.responder(s.getId(), new ResponderSolicitudRequest(DecisionSolicitud.ACEPTAR, null));

        ArgumentCaptor<Asesoria> captor = ArgumentCaptor.forClass(Asesoria.class);
        verify(asesoriaRepository).save(captor.capture());
        assertEquals("COASESOR", captor.getValue().getTipo());
        assertEquals(docP, captor.getValue().getDocenteId());
        // El co-asesor también recibe el rol: entra a la bandeja, pero el detalle le llega en
        // modo consulta (AsesorProyectoServiceImpl marca soloLectura y bloquea la escritura).
        verify(asesorRolService).otorgarRolAsesor(docP);
    }

    @Test
    void responder_aceptarAsesoria_cuandoYaHayAsesor_lanzaBusiness() {
        UUID docP = UUID.randomUUID();
        UUID estP = UUID.randomUUID();
        UUID tesisId = UUID.randomUUID();
        stubDocenteAutenticado(docP);

        SolicitudAsesoria s = solicitudPendiente(docP, estP);
        s.setTipo(TipoAsesoria.ASESOR);
        when(solicitudRepository.buscarPorId(s.getId())).thenReturn(Optional.of(s));
        when(tesisAutorRepository.tesisActivaId(estP)).thenReturn(tesisId);
        when(asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR"))
                .thenReturn(Optional.of(asesoriaDe(tesisId, UUID.randomUUID(), "ASESOR")));

        assertThrows(BusinessException.class, () -> service.responder(s.getId(),
                new ResponderSolicitudRequest(DecisionSolicitud.ACEPTAR, null)));
        verify(asesoriaRepository, never()).save(any());
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
