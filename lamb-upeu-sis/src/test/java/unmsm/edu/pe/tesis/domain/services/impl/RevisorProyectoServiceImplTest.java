package unmsm.edu.pe.tesis.domain.services.impl;

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
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.EvaluarRevisorRequest;
import unmsm.edu.pe.tesis.application.dto.ObservarItemRequest;
import unmsm.edu.pe.tesis.application.util.RubricaDefinicion;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevision;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoItemRevision;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisionEventoRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisionRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRubricaPuntajeRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevisorProyectoServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock PersonaRepository personaRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock ProyectoTesisRepository proyectoRepository;
    @Mock ProyectoRevisorRepository revisorRepository;
    @Mock ProyectoRubricaPuntajeRepository puntajeRepository;
    @Mock ProyectoRevisionRepository revisionRepository;
    @Mock ProyectoRevisionEventoRepository eventoRepository;
    @Mock DocumentoTesisRepository documentoTesisRepository;
    @Mock AlmacenamientoArchivos almacenamiento;

    @InjectMocks RevisorProyectoServiceImpl service;

    private final UUID USER = UUID.randomUUID();
    private final UUID DOC = UUID.randomUUID();
    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private final UUID REV = UUID.randomUUID();

    // La rúbrica cuantitativa tiene 24 criterios; CUMPLE=100, PARCIAL=65, NO_CUMPLE=37.
    private static final int NUM_CRITERIOS = RubricaDefinicion.CUANTITATIVA.criterios().size();

    private ProyectoRevisor revisor;

    @BeforeEach
    void setUp() {
        Persona persona = new Persona();
        persona.setId(DOC);
        Docente docente = new Docente();
        docente.setPersonaId(DOC);
        docente.setPersona(persona);
        when(securityUtils.getCurrentUserIdAsUUID()).thenReturn(USER);
        when(personaRepository.findByUserId(USER)).thenReturn(Optional.of(persona));
        when(docenteRepository.findByPersonaId(DOC)).thenReturn(Optional.of(docente));

        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));

        revisor = ProyectoRevisor.builder().id(REV).proyectoId(PROY).docenteId(DOC)
                .estado(EstadoRevisor.DESIGNADO).build();
        lenient().when(revisorRepository.buscarPorProyectoYDocente(PROY, DOC)).thenReturn(Optional.of(revisor));
        // La rúbrica oficial ya está subida (habilitada) por defecto.
        lenient().when(documentoTesisRepository.existePorTesisYTipo(any(), any())).thenReturn(true);
    }

    /** Todos los criterios de la rúbrica cuantitativa en un mismo nivel. */
    private Map<String, String> niveles(RubricaDefinicion.Nivel nivel) {
        Map<String, String> m = new HashMap<>();
        for (RubricaDefinicion.Criterio c : RubricaDefinicion.CUANTITATIVA.criterios()) {
            m.put(c.key(), nivel.name());
        }
        return m;
    }

    private EvaluarRevisorRequest req(Map<String, String> niveles, String comentario, boolean conforme) {
        EvaluarRevisorRequest r = new EvaluarRevisorRequest();
        r.setNiveles(niveles); r.setComentario(comentario); r.setConforme(conforme);
        return r;
    }

    @Test
    void evaluar_observar_conComentario_guardaObservadoYTotal() {
        service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.PARCIAL), "Precisar la muestra", false));

        assertEquals(EstadoRevisor.OBSERVADO, revisor.getEstado());
        assertEquals(65, revisor.getPuntajeTotal());   // todos PARCIAL
        verify(puntajeRepository).eliminarPorRevisor(REV);
        verify(puntajeRepository, times(NUM_CRITERIOS)).save(any());
    }

    @Test
    void evaluar_conformidad_guardaConformeYFecha() {
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(java.util.List.of(revisor));
        service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.CUMPLE), null, true));

        assertEquals(EstadoRevisor.CONFORME, revisor.getEstado());
        assertEquals(100, revisor.getPuntajeTotal());   // todos CUMPLE
        assertNotNull(revisor.getFechaConformidad());
    }

    @Test
    void evaluar_conformidadConObservaciones_falla() {
        // Si registró observaciones por criterio, no puede dar conformidad: solo Observar.
        String algunCriterio = RubricaDefinicion.CUANTITATIVA.criterios().get(0).key();
        EvaluarRevisorRequest r = req(niveles(RubricaDefinicion.Nivel.CUMPLE), null, true);
        r.setObservaciones(Map.of(algunCriterio, "Corrige la muestra"));
        assertThrows(ValidationException.class, () -> service.evaluar(TESIS, r));
    }

    @Test
    void evaluar_ambosConformes_marcaProyectoListo() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
        ProyectoRevisor otro = ProyectoRevisor.builder().id(UUID.randomUUID()).proyectoId(PROY)
                .estado(EstadoRevisor.CONFORME).build();
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(java.util.List.of(revisor, otro));

        service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.CUMPLE), null, true));

        assertTrue(Boolean.TRUE.equals(p.getRevisoresConformes()));
        assertNotNull(p.getFechaRevisoresConformes());
    }

    @Test
    void evaluar_conformidadPeroFaltaOtroRevisor_noMarcaListo() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
        ProyectoRevisor otro = ProyectoRevisor.builder().id(UUID.randomUUID()).proyectoId(PROY)
                .estado(EstadoRevisor.DESIGNADO).build();
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(java.util.List.of(revisor, otro));

        service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.CUMPLE), null, true));

        assertNull(p.getRevisoresConformes());
    }

    @Test
    void evaluar_observarSinComentario_falla() {
        assertThrows(ValidationException.class,
                () -> service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.PARCIAL), "  ", false)));
        verify(revisorRepository, never()).save(any());
    }

    @Test
    void evaluar_criterioFaltante_falla() {
        Map<String, String> incompleta = niveles(RubricaDefinicion.Nivel.PARCIAL);
        incompleta.remove("situacion");
        assertThrows(ValidationException.class,
                () -> service.evaluar(TESIS, req(incompleta, "x", false)));
    }

    @Test
    void evaluar_nivelInvalido_falla() {
        Map<String, String> m = niveles(RubricaDefinicion.Nivel.PARCIAL);
        m.put("situacion", "EXCELENTE");
        assertThrows(ValidationException.class, () -> service.evaluar(TESIS, req(m, "x", false)));
    }

    @Test
    void evaluar_rubricaVacia_falla() {
        assertThrows(ValidationException.class,
                () -> service.evaluar(TESIS, req(new HashMap<>(), "x", false)));
    }

    @Test
    void evaluar_rubricaNoSubida_falla() {
        when(documentoTesisRepository.existePorTesisYTipo(any(), any())).thenReturn(false);
        assertThrows(BusinessException.class,
                () -> service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.CUMPLE), "x", false)));
        verify(revisorRepository, never()).save(any());
    }

    @Test
    void evaluar_yaConforme_falla() {
        revisor.setEstado(EstadoRevisor.CONFORME);
        assertThrows(BusinessException.class,
                () -> service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.CUMPLE), null, true)));
    }

    @Test
    void evaluar_noEsRevisor_falla() {
        when(revisorRepository.buscarPorProyectoYDocente(PROY, DOC)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.PARCIAL), "x", false)));
    }

    // ── Observación por ítem ──

    private ObservarItemRequest obsReq(String campo, String texto) {
        ObservarItemRequest r = new ObservarItemRequest();
        r.setCampo(campo); r.setTexto(texto);
        return r;
    }

    @Test
    void observarItem_marcaObservadoYRegistraEventoDeRevisor() {
        when(revisionRepository.buscarPorProyectoYCampo(PROY, "objGeneral")).thenReturn(Optional.empty());
        when(revisionRepository.save(any())).thenAnswer(i -> {
            ProyectoRevision r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });

        service.observarItem(TESIS, obsReq("objGeneral", "Precisa el objetivo general"));

        assertEquals(EstadoRevisor.OBSERVADO, revisor.getEstado());
        verify(revisionRepository).save(argThat(r -> r.getEstado() == EstadoItemRevision.OBSERVADO));
        verify(eventoRepository).save(argThat(e -> "REVISOR".equals(e.getRol()) && "OBSERVACIÓN".equals(e.getTipo())));
    }

    @Test
    void observarItem_rubricaNoSubida_falla() {
        when(documentoTesisRepository.existePorTesisYTipo(any(), any())).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.observarItem(TESIS, obsReq("objGeneral", "x")));
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void observarItem_yaConforme_falla() {
        revisor.setEstado(EstadoRevisor.CONFORME);
        assertThrows(BusinessException.class, () -> service.observarItem(TESIS, obsReq("objGeneral", "x")));
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void observarItem_sinTexto_falla() {
        assertThrows(ValidationException.class, () -> service.observarItem(TESIS, obsReq("objGeneral", "  ")));
    }

    @Test
    void darConformidadItem_marcaConformeYRegistraEvento() {
        ProyectoRevision rev = ProyectoRevision.builder().id(UUID.randomUUID()).proyectoId(PROY)
                .campo("objGeneral").estado(EstadoItemRevision.CORREGIDO).build();
        when(revisionRepository.buscarPorProyectoYCampo(PROY, "objGeneral")).thenReturn(java.util.Optional.of(rev));

        service.darConformidadItem(TESIS, "objGeneral");

        assertEquals(EstadoItemRevision.CONFORME, rev.getEstado());
        verify(revisionRepository).save(rev);
        verify(eventoRepository).save(argThat(e -> "REVISOR".equals(e.getRol()) && "CONFORMIDAD".equals(e.getTipo())));
    }

    @Test
    void darConformidadItem_yaConforme_noHaceNada() {
        ProyectoRevision rev = ProyectoRevision.builder().id(UUID.randomUUID()).proyectoId(PROY)
                .campo("objGeneral").estado(EstadoItemRevision.CONFORME).build();
        when(revisionRepository.buscarPorProyectoYCampo(PROY, "objGeneral")).thenReturn(java.util.Optional.of(rev));

        service.darConformidadItem(TESIS, "objGeneral");

        verify(eventoRepository, never()).save(any());
    }

    @Test
    void darConformidadItem_revisorYaConforme_falla() {
        revisor.setEstado(EstadoRevisor.CONFORME);
        assertThrows(BusinessException.class, () -> service.darConformidadItem(TESIS, "objGeneral"));
    }

    // ── Candado de etapa: bloqueado si el proyecto ya avanzó (defensa/informe) ──

    @Test
    void observarItem_conDefensaProgramada_falla() {
        ProyectoTesis avanzado = ProyectoTesis.builder().id(PROY).tesisId(TESIS).defensaProgramada(true).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(avanzado));
        assertThrows(BusinessException.class, () -> service.observarItem(TESIS, obsReq("objGeneral", "x")));
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void evaluar_conDefensaProgramada_falla() {
        ProyectoTesis avanzado = ProyectoTesis.builder().id(PROY).tesisId(TESIS).defensaProgramada(true).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(avanzado));
        assertThrows(BusinessException.class,
                () -> service.evaluar(TESIS, req(niveles(RubricaDefinicion.Nivel.CUMPLE), "x", false)));
        verify(revisorRepository, never()).save(any());
    }

    @Test
    void darConformidadItem_conInformeFinalRevisado_falla() {
        ProyectoTesis avanzado = ProyectoTesis.builder().id(PROY).tesisId(TESIS).informeFinalRevisado(true).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(avanzado));
        assertThrows(BusinessException.class, () -> service.darConformidadItem(TESIS, "objGeneral"));
    }

}
