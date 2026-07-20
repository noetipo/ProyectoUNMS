package unmsm.edu.pe.tesis.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.DesignarRevisoresRequest;
import unmsm.edu.pe.tesis.application.dto.ProgramarDefensaRequest;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.ProyectoJurado;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoJuradoRepository;
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
    @Mock ProyectoJuradoRepository juradoRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock PersonaRepository personaRepository;

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

    // ── programarDefensa ──
    private ProgramarDefensaRequest defReq(UUID presidente, UUID m1, UUID m2) {
        ProgramarDefensaRequest r = new ProgramarDefensaRequest();
        r.setPresidenteId(presidente);
        r.setMiembroIds(java.util.List.of(m1, m2));
        r.setFecha(java.time.LocalDate.of(2026, 8, 15));
        r.setHora("10:00");
        r.setLugar("Auditorio");
        return r;
    }

    private ProyectoTesis proyectoConformes() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS)
                .expedienteRecibido(true).revisoresConformes(true).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void programar_creaJuradoConAsesorYMarcaProgramada() {
        ProyectoTesis p = proyectoConformes();
        when(asesoriaRepository.buscarPorTesisYTipo(TESIS, "ASESOR"))
                .thenReturn(Optional.of(Asesoria.builder().docenteId(ASESOR).build()));
        when(docenteRepository.findByPersonaId(any())).thenReturn(Optional.of(new Docente()));

        service.programarDefensa(TESIS, defReq(DOC_A, DOC_B, DOC_C));

        // Presidente + 2 miembros + asesor = 4
        verify(juradoRepository, times(4)).save(any(ProyectoJurado.class));
        assertTrue(Boolean.TRUE.equals(p.getDefensaProgramada()));
        assertEquals(java.time.LocalDate.of(2026, 8, 15), p.getFechaDefensa());
        assertNotNull(p.getDictamenNumero());
    }

    @Test
    void programar_sinRevisoresConformes_falla() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS)
                .expedienteRecibido(true).revisoresConformes(false).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));

        assertThrows(BusinessException.class, () -> service.programarDefensa(TESIS, defReq(DOC_A, DOC_B, DOC_C)));
        verify(juradoRepository, never()).save(any());
    }

    @Test
    void programar_yaProgramada_falla() {
        ProyectoTesis p = proyectoConformes();
        p.setDefensaProgramada(true);

        assertThrows(BusinessException.class, () -> service.programarDefensa(TESIS, defReq(DOC_A, DOC_B, DOC_C)));
    }

    @Test
    void programar_presidenteEsMiembro_falla() {
        proyectoConformes();
        assertThrows(ValidationException.class, () -> service.programarDefensa(TESIS, defReq(DOC_A, DOC_A, DOC_B)));
    }

    @Test
    void programar_miembrosDuplicados_falla() {
        proyectoConformes();
        assertThrows(ValidationException.class, () -> service.programarDefensa(TESIS, defReq(DOC_A, DOC_B, DOC_B)));
    }
}
