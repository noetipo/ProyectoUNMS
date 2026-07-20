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
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.EvaluarRevisorRequest;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
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

    @InjectMocks RevisorProyectoServiceImpl service;

    private final UUID USER = UUID.randomUUID();
    private final UUID DOC = UUID.randomUUID();
    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private final UUID REV = UUID.randomUUID();

    private ProyectoRevisor revisor;

    @BeforeEach
    void setUp() {
        // Resolución del docente actual
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
    }

    private Map<String, Integer> rubricaCompleta(int v) {
        Map<String, Integer> m = new HashMap<>();
        m.put("problema", v); m.put("marco", v); m.put("metodologia", v);
        m.put("viabilidad", v); m.put("redaccion", v);
        return m;
    }

    private EvaluarRevisorRequest req(Map<String, Integer> p, String comentario, boolean conforme) {
        EvaluarRevisorRequest r = new EvaluarRevisorRequest();
        r.setPuntajes(p); r.setComentario(comentario); r.setConforme(conforme);
        return r;
    }

    @Test
    void evaluar_observar_conComentario_guardaObservadoYTotal() {
        service.evaluar(TESIS, req(rubricaCompleta(3), "Precisar la muestra", false));

        assertEquals(EstadoRevisor.OBSERVADO, revisor.getEstado());
        assertEquals(15, revisor.getPuntajeTotal());   // 3*5
        verify(puntajeRepository).eliminarPorRevisor(REV);
        verify(puntajeRepository, times(5)).save(any());
    }

    @Test
    void evaluar_conformidad_guardaConformeYFecha() {
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(java.util.List.of(revisor));
        service.evaluar(TESIS, req(rubricaCompleta(4), null, true));

        assertEquals(EstadoRevisor.CONFORME, revisor.getEstado());
        assertEquals(20, revisor.getPuntajeTotal());
        assertNotNull(revisor.getFechaConformidad());
    }

    @Test
    void evaluar_ambosConformes_marcaProyectoListo() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
        ProyectoRevisor otro = ProyectoRevisor.builder().id(UUID.randomUUID()).proyectoId(PROY)
                .estado(EstadoRevisor.CONFORME).build();
        // tras dar conformidad, ambos revisores quedan CONFORME
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(java.util.List.of(revisor, otro));

        service.evaluar(TESIS, req(rubricaCompleta(4), null, true));

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

        service.evaluar(TESIS, req(rubricaCompleta(4), null, true));

        assertNull(p.getRevisoresConformes());
    }

    @Test
    void evaluar_observarSinComentario_falla() {
        assertThrows(ValidationException.class,
                () -> service.evaluar(TESIS, req(rubricaCompleta(3), "  ", false)));
        verify(revisorRepository, never()).save(any());
    }

    @Test
    void evaluar_criterioFaltante_falla() {
        Map<String, Integer> incompleta = rubricaCompleta(3);
        incompleta.remove("metodologia");
        assertThrows(ValidationException.class,
                () -> service.evaluar(TESIS, req(incompleta, "x", false)));
    }

    @Test
    void evaluar_puntajeFueraDeRango_falla() {
        assertThrows(ValidationException.class,
                () -> service.evaluar(TESIS, req(rubricaCompleta(5), "x", false)));
    }

    @Test
    void evaluar_rubricaVacia_falla() {
        assertThrows(ValidationException.class,
                () -> service.evaluar(TESIS, req(new HashMap<>(), "x", false)));
    }

    @Test
    void evaluar_yaConforme_falla() {
        revisor.setEstado(EstadoRevisor.CONFORME);
        assertThrows(BusinessException.class,
                () -> service.evaluar(TESIS, req(rubricaCompleta(4), null, true)));
    }

    @Test
    void evaluar_noEsRevisor_falla() {
        when(revisorRepository.buscarPorProyectoYDocente(PROY, DOC)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class,
                () -> service.evaluar(TESIS, req(rubricaCompleta(3), "x", false)));
    }
}
