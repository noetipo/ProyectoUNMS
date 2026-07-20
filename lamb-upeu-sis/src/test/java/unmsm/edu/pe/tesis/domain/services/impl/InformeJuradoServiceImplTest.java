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
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.EvaluarInformeRequest;
import unmsm.edu.pe.tesis.domain.entities.InformeRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.repositories.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InformeJuradoServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock PersonaRepository personaRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock EstudianteRepository estudianteRepository;
    @Mock TesisAutorRepository tesisAutorRepository;
    @Mock ProyectoTesisRepository proyectoRepository;
    @Mock TesisRepository tesisRepository;
    @Mock InformeRevisorRepository informeRevisorRepository;
    @Mock DocumentoTesisRepository documentoTesisRepository;

    @InjectMocks InformeJuradoServiceImpl service;

    private final UUID USER = UUID.randomUUID();
    private final UUID DOC = UUID.randomUUID();
    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private ProyectoTesis proyecto;
    private InformeRevisor revisor;

    @BeforeEach
    void setUp() {
        Persona persona = new Persona();
        persona.setId(DOC);
        Docente docente = new Docente();
        docente.setPersonaId(DOC);
        when(securityUtils.getCurrentUserIdAsUUID()).thenReturn(USER);
        when(personaRepository.findByUserId(USER)).thenReturn(Optional.of(persona));
        when(docenteRepository.findByPersonaId(DOC)).thenReturn(Optional.of(docente));

        proyecto = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(proyecto));

        revisor = InformeRevisor.builder().id(UUID.randomUUID()).proyectoId(PROY).docenteId(DOC)
                .estado(EstadoRevisor.DESIGNADO).build();
        lenient().when(informeRevisorRepository.buscarPorProyectoYDocente(PROY, DOC)).thenReturn(Optional.of(revisor));
    }

    private EvaluarInformeRequest req(Integer puntaje, String comentario, boolean conforme) {
        EvaluarInformeRequest r = new EvaluarInformeRequest();
        r.setPuntaje(puntaje); r.setComentario(comentario); r.setConforme(conforme);
        return r;
    }

    @Test
    void evaluar_observar_conComentario() {
        service.evaluar(TESIS, req(12, "Mejorar discusión", false));
        assertEquals(EstadoRevisor.OBSERVADO, revisor.getEstado());
        assertEquals(12, revisor.getPuntaje());
    }

    @Test
    void evaluar_conformidad() {
        when(informeRevisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(revisor));
        service.evaluar(TESIS, req(18, null, true));
        assertEquals(EstadoRevisor.CONFORME, revisor.getEstado());
        assertNotNull(revisor.getFechaConformidad());
    }

    @Test
    void evaluar_observarSinComentario_falla() {
        assertThrows(ValidationException.class, () -> service.evaluar(TESIS, req(10, " ", false)));
    }

    @Test
    void evaluar_puntajeFueraDeRango_falla() {
        assertThrows(ValidationException.class, () -> service.evaluar(TESIS, req(25, "x", false)));
    }

    @Test
    void evaluar_sinPuntaje_falla() {
        assertThrows(ValidationException.class, () -> service.evaluar(TESIS, req(null, "x", false)));
    }

    @Test
    void evaluar_yaConforme_falla() {
        revisor.setEstado(EstadoRevisor.CONFORME);
        assertThrows(BusinessException.class, () -> service.evaluar(TESIS, req(18, null, true)));
    }

    @Test
    void evaluar_noEsJurado_falla() {
        when(informeRevisorRepository.buscarPorProyectoYDocente(PROY, DOC)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.evaluar(TESIS, req(15, "x", false)));
    }

    @Test
    void evaluar_losTresConformes_marcaInformeRevisado() {
        InformeRevisor r2 = InformeRevisor.builder().id(UUID.randomUUID()).proyectoId(PROY).estado(EstadoRevisor.CONFORME).build();
        InformeRevisor r3 = InformeRevisor.builder().id(UUID.randomUUID()).proyectoId(PROY).estado(EstadoRevisor.CONFORME).build();
        when(informeRevisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(revisor, r2, r3));

        service.evaluar(TESIS, req(17, null, true));

        assertTrue(Boolean.TRUE.equals(proyecto.getInformeFinalRevisado()));
        assertNotNull(proyecto.getFechaInformeRevisado());
    }
}
