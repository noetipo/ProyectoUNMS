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
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.DesignarRevisoresRequest;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.enums.EstadoRevisor;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoordinadorProyectoServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock ProyectoTesisRepository proyectoRepository;
    @Mock ProyectoRevisorRepository revisorRepository;
    @Mock AsesoriaRepository asesoriaRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock PersonaRepository personaRepository;
    @Mock AsesorRolService asesorRolService;

    @InjectMocks CoordinadorProyectoServiceImpl service;

    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private final UUID ASESOR = UUID.randomUUID();
    private final UUID DOC_A = UUID.randomUUID();
    private final UUID DOC_B = UUID.randomUUID();
    private final UUID DOC_C = UUID.randomUUID();

    private DesignarRevisoresRequest req(UUID... ids) {
        DesignarRevisoresRequest r = new DesignarRevisoresRequest();
        r.setDocenteIds(List.of(ids));
        return r;
    }

    private void proyectoRecepcionado() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS)
                .expedienteRecibido(true).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
    }

    private void docentesExisten() {
        when(docenteRepository.findByPersonaId(any())).thenReturn(Optional.of(new Docente()));
    }

    @Test
    void designar_dosRevisoresValidos_guardaEnOrden() {
        proyectoRecepcionado();
        when(revisorRepository.contarPorProyecto(PROY)).thenReturn(0L);
        when(asesoriaRepository.buscarPorTesisYTipo(TESIS, "ASESOR"))
                .thenReturn(Optional.of(Asesoria.builder().docenteId(ASESOR).build()));
        docentesExisten();

        service.designarRevisores(TESIS, req(DOC_A, DOC_B));

        ArgumentCaptor<ProyectoRevisor> cap = ArgumentCaptor.forClass(ProyectoRevisor.class);
        verify(revisorRepository, times(2)).save(cap.capture());
        List<ProyectoRevisor> saved = cap.getAllValues();
        assertEquals(DOC_A, saved.get(0).getDocenteId());
        assertEquals(1, saved.get(0).getOrden());
        assertEquals(DOC_B, saved.get(1).getDocenteId());
        assertEquals(2, saved.get(1).getOrden());
    }

    @Test
    void designar_noRecepcionado_falla() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).expedienteRecibido(false).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));

        assertThrows(BusinessException.class, () -> service.designarRevisores(TESIS, req(DOC_A, DOC_B)));
        verify(revisorRepository, never()).save(any());
    }

    @Test
    void designar_yaTieneRevisores_falla() {
        proyectoRecepcionado();
        when(revisorRepository.contarPorProyecto(PROY)).thenReturn(2L);

        assertThrows(BusinessException.class, () -> service.designarRevisores(TESIS, req(DOC_A, DOC_B)));
        verify(revisorRepository, never()).save(any());
    }

    @Test
    void designar_menosDeDos_falla() {
        proyectoRecepcionado();
        when(revisorRepository.contarPorProyecto(PROY)).thenReturn(0L);

        assertThrows(ValidationException.class, () -> service.designarRevisores(TESIS, req(DOC_A)));
    }

    @Test
    void designar_duplicados_seDedupYFalla() {
        proyectoRecepcionado();
        when(revisorRepository.contarPorProyecto(PROY)).thenReturn(0L);

        // dos ids iguales → tras dedup queda 1 → ValidationException (no son 2 distintos)
        assertThrows(ValidationException.class, () -> service.designarRevisores(TESIS, req(DOC_A, DOC_A)));
    }

    @Test
    void designar_asesorComoRevisor_falla() {
        proyectoRecepcionado();
        when(revisorRepository.contarPorProyecto(PROY)).thenReturn(0L);
        when(asesoriaRepository.buscarPorTesisYTipo(TESIS, "ASESOR"))
                .thenReturn(Optional.of(Asesoria.builder().docenteId(ASESOR).build()));
        docentesExisten();

        assertThrows(BusinessException.class, () -> service.designarRevisores(TESIS, req(DOC_A, ASESOR)));
    }

    @Test
    void designar_docenteInexistente_falla() {
        proyectoRecepcionado();
        when(revisorRepository.contarPorProyecto(PROY)).thenReturn(0L);
        when(asesoriaRepository.buscarPorTesisYTipo(TESIS, "ASESOR")).thenReturn(Optional.empty());
        when(docenteRepository.findByPersonaId(any())).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.designarRevisores(TESIS, req(DOC_A, DOC_B)));
    }

    // ── defensa(): solo lectura, informa fecha/modalidad y quién evalúa ──
    // Programarla es cosa de Secretaría (SecretariaDefensaServiceImpl); el Coordinador ya NO
    // elige un jurado aparte aquí — evalúan los mismos dos revisores del proyecto (ver el
    // diagrama del proceso: no hay jurado propio para la defensa del proyecto, Etapa 5).

    @Test
    void defensa_listaLosRevisoresComoEvaluadores() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS)
                .defensaProgramada(true).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));

        ProyectoRevisor r1 = ProyectoRevisor.builder().docenteId(DOC_A).orden(1).estado(EstadoRevisor.CONFORME).build();
        ProyectoRevisor r2 = ProyectoRevisor.builder().docenteId(DOC_B).orden(2).estado(EstadoRevisor.CONFORME).build();
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(r1, r2));

        Persona pa = new Persona(); pa.setId(DOC_A); pa.setNombres("Ana"); pa.setApellidoPaterno("Reyes");
        Persona pb = new Persona(); pb.setId(DOC_B); pb.setNombres("Luis"); pb.setApellidoPaterno("Soto");
        when(personaRepository.buscarPorId(DOC_A)).thenReturn(Optional.of(pa));
        when(personaRepository.buscarPorId(DOC_B)).thenReturn(Optional.of(pb));

        var info = service.defensa(TESIS);

        assertEquals(2, info.getJurado().size());
        assertTrue(info.getJurado().stream().allMatch(j -> "REVISOR".equals(j.getRol())));
        assertEquals(DOC_A, info.getJurado().get(0).getDocenteId());
        verifyNoInteractions(docenteRepository); // no valida/crea jurado: solo lee revisores
    }

    @Test
    void defensa_sinRevisoresDesignados_juradoVacio() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(List.of());

        var info = service.defensa(TESIS);

        assertTrue(info.getJurado().isEmpty());
        assertFalse(info.isProgramada());
    }
}
