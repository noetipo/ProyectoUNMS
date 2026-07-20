package unmsm.edu.pe.tesis.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import unmsm.edu.pe.tesis.application.dto.RegistrarAvanceRequest;
import unmsm.edu.pe.tesis.domain.entities.ProyectoActividad;
import unmsm.edu.pe.tesis.domain.entities.ProyectoAvance;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoActividad;
import unmsm.edu.pe.tesis.domain.repositories.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsesorEjecucionServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock PersonaRepository personaRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock EstudianteRepository estudianteRepository;
    @Mock TesisRepository tesisRepository;
    @Mock TesisAutorRepository tesisAutorRepository;
    @Mock ProyectoTesisRepository proyectoRepository;
    @Mock ProyectoActividadRepository actividadRepository;
    @Mock ProyectoAvanceRepository avanceRepository;
    @Mock DocumentoTesisRepository documentoTesisRepository;
    @Mock ProyectoEditorAssembler assembler;

    @InjectMocks AsesorEjecucionServiceImpl service;

    private final UUID USER = UUID.randomUUID();
    private final UUID ASESOR = UUID.randomUUID();
    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private ProyectoTesis proyecto;

    @BeforeEach
    void setUp() {
        Persona persona = new Persona();
        persona.setId(ASESOR);
        Docente docente = new Docente();
        docente.setPersonaId(ASESOR);
        when(securityUtils.getCurrentUserIdAsUUID()).thenReturn(USER);
        when(personaRepository.findByUserId(USER)).thenReturn(Optional.of(persona));
        when(docenteRepository.findByPersonaId(ASESOR)).thenReturn(Optional.of(docente));

        proyecto = ProyectoTesis.builder().id(PROY).tesisId(TESIS).asesorId(ASESOR).defensaProgramada(true).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(proyecto));
    }

    private void planEjecutado(int hechas, int total) {
        java.util.List<ProyectoActividad> acts = new java.util.ArrayList<>();
        for (int i = 0; i < total; i++) {
            acts.add(ProyectoActividad.builder()
                    .estado(i < hechas ? EstadoActividad.HECHA : EstadoActividad.EN_CURSO).build());
        }
        when(actividadRepository.listarPorProyecto(PROY)).thenReturn(acts);
    }

    private RegistrarAvanceRequest avanceReq(int v, String comentario) {
        RegistrarAvanceRequest r = new RegistrarAvanceRequest();
        Map<String, Integer> m = new HashMap<>();
        m.put("ejecucion", v); m.put("datos", v); m.put("analisis", v); m.put("interpretacion", v);
        r.setPuntajes(m);
        r.setComentario(comentario);
        return r;
    }

    // ── registrarAvance ──
    @Test
    void registrarAvance_valido_guardaConPorcentaje() {
        planEjecutado(1, 2); // 50%
        service.registrarAvance(TESIS, avanceReq(3, "avance ok"));

        ArgumentCaptor<ProyectoAvance> cap = ArgumentCaptor.forClass(ProyectoAvance.class);
        verify(avanceRepository).save(cap.capture());
        ProyectoAvance a = cap.getValue();
        assertEquals(3, a.getPuntajeEjecucion());
        assertEquals(50, a.getPorcentajePlan());
    }

    @Test
    void registrarAvance_criterioFaltante_falla() {
        RegistrarAvanceRequest r = avanceReq(3, "x");
        r.getPuntajes().remove("datos");
        assertThrows(ValidationException.class, () -> service.registrarAvance(TESIS, r));
        verify(avanceRepository, never()).save(any());
    }

    @Test
    void registrarAvance_puntajeFueraDeRango_falla() {
        assertThrows(ValidationException.class, () -> service.registrarAvance(TESIS, avanceReq(5, "x")));
    }

    @Test
    void registrarAvance_noEsAsesor_falla() {
        proyecto.setAsesorId(UUID.randomUUID());
        assertThrows(BusinessException.class, () -> service.registrarAvance(TESIS, avanceReq(3, "x")));
    }

    // ── aprobarInformeFinal ──
    @Test
    void aprobar_conPlan100YInformeSubido_aprueba() {
        planEjecutado(2, 2); // 100%
        when(documentoTesisRepository.existePorTesisYTipo(TESIS, "INFORME_FINAL_TESIS")).thenReturn(true);

        service.aprobarInformeFinal(TESIS);

        assertTrue(Boolean.TRUE.equals(proyecto.getInformeFinalAprobado()));
        assertNotNull(proyecto.getFechaInformeFinal());
    }

    @Test
    void aprobar_planIncompleto_falla() {
        planEjecutado(1, 2); // 50%
        assertThrows(BusinessException.class, () -> service.aprobarInformeFinal(TESIS));
    }

    @Test
    void aprobar_sinInformeSubido_falla() {
        planEjecutado(2, 2);
        when(documentoTesisRepository.existePorTesisYTipo(TESIS, "INFORME_FINAL_TESIS")).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.aprobarInformeFinal(TESIS));
    }

    @Test
    void aprobar_yaAprobado_falla() {
        proyecto.setInformeFinalAprobado(true);
        assertThrows(BusinessException.class, () -> service.aprobarInformeFinal(TESIS));
    }
}
