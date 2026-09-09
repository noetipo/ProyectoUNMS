package unmsm.edu.pe.tesis.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.storage.AlmacenamientoArchivos;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.exceptions.ValidationException;
import unmsm.edu.pe.tesis.application.dto.ProgramarDefensaRequest;
import unmsm.edu.pe.tesis.domain.entities.ProyectoRevisor;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.repositories.DocumentoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.PlantillaRubricaRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoRevisorRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;
import unmsm.edu.pe.tesis.domain.repositories.RubricaDefensaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * La Secretaría programa la defensa (Etapa 5) con solo modalidad, fecha y hora — quiénes la
 * evalúan NO se eligen aquí: son los mismos dos revisores ya designados (ver el diagrama del
 * proceso; el jurado propio es de la Sustentación final, Etapa 8).
 */
@ExtendWith(MockitoExtension.class)
class SecretariaDefensaServiceImplTest {

    @Mock ProyectoTesisRepository proyectoRepository;
    @Mock ProyectoRevisorRepository revisorRepository;
    @Mock RubricaDefensaRepository rubricaRepository;
    @Mock DocumentoTesisRepository documentoTesisRepository;
    @Mock AlmacenamientoArchivos almacenamiento;
    @Mock PersonaRepository personaRepository;
    @Mock PlantillaRubricaRepository plantillaRepository;

    @InjectMocks SecretariaDefensaServiceImpl service;

    private final UUID TESIS = UUID.randomUUID();
    private final UUID PROY = UUID.randomUUID();
    private final UUID DOC_A = UUID.randomUUID();
    private final UUID DOC_B = UUID.randomUUID();

    private ProyectoTesis proyectoConformes() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).revisoresConformes(true).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));
        return p;
    }

    private ProgramarDefensaRequest req(String modalidad, LocalDate fecha, String lugar, String enlace) {
        ProgramarDefensaRequest r = new ProgramarDefensaRequest();
        r.setModalidad(modalidad);
        r.setFecha(fecha);
        r.setHora("10:00");
        r.setLugar(lugar);
        r.setEnlace(enlace);
        return r;
    }

    // ── programarDefensa ──

    @Test
    void programar_presencialConLugar_programaYGeneraDictamen() {
        ProyectoTesis p = proyectoConformes();

        service.programarDefensa(TESIS, req("PRESENCIAL", LocalDate.of(2026, 9, 1), "Auditorio 3", null));

        assertTrue(Boolean.TRUE.equals(p.getDefensaProgramada()));
        assertEquals(LocalDate.of(2026, 9, 1), p.getFechaDefensa());
        assertEquals("Auditorio 3", p.getLugarDefensa());
        assertNull(p.getEnlaceDefensa());
        assertNotNull(p.getDictamenNumero());
        verify(proyectoRepository).save(p);
    }

    @Test
    void programar_virtualConEnlace_programaSinLugar() {
        proyectoConformes();

        service.programarDefensa(TESIS, req("VIRTUAL", LocalDate.of(2026, 9, 1), null, "https://meet.example/x"));

        verify(proyectoRepository).save(argThat(p ->
                Boolean.TRUE.equals(p.getDefensaProgramada())
                        && p.getLugarDefensa() == null
                        && "https://meet.example/x".equals(p.getEnlaceDefensa())));
    }

    @Test
    void programar_hibridaRequiereLugarYEnlace() {
        proyectoConformes();
        assertThrows(ValidationException.class,
                () -> service.programarDefensa(TESIS, req("HIBRIDA", LocalDate.of(2026, 9, 1), "Auditorio 3", null)));
        assertThrows(ValidationException.class,
                () -> service.programarDefensa(TESIS, req("HIBRIDA", LocalDate.of(2026, 9, 1), null, "https://meet.example/x")));
    }

    @Test
    void programar_hibridaConAmbos_programa() {
        proyectoConformes();
        service.programarDefensa(TESIS, req("HIBRIDA", LocalDate.of(2026, 9, 1), "Auditorio 3", "https://meet.example/x"));
        verify(proyectoRepository).save(any());
    }

    @Test
    void programar_presencialSinLugar_falla() {
        proyectoConformes();
        assertThrows(ValidationException.class,
                () -> service.programarDefensa(TESIS, req("PRESENCIAL", LocalDate.of(2026, 9, 1), null, null)));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void programar_virtualSinEnlace_falla() {
        proyectoConformes();
        assertThrows(ValidationException.class,
                () -> service.programarDefensa(TESIS, req("VIRTUAL", LocalDate.of(2026, 9, 1), null, null)));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void programar_sinFecha_falla() {
        proyectoConformes();
        assertThrows(ValidationException.class,
                () -> service.programarDefensa(TESIS, req("PRESENCIAL", null, "Auditorio 3", null)));
    }

    @Test
    void programar_modalidadInvalida_falla() {
        proyectoConformes();
        assertThrows(ValidationException.class,
                () -> service.programarDefensa(TESIS, req("HOLOGRAMA", LocalDate.of(2026, 9, 1), "Auditorio 3", null)));
    }

    @Test
    void programar_sinModalidad_asumePresencial() {
        proyectoConformes();
        // Compatibilidad con lo que se venía programando antes de que existiera el campo modalidad.
        service.programarDefensa(TESIS, req(null, LocalDate.of(2026, 9, 1), "Auditorio 3", null));
        verify(proyectoRepository).save(any());
    }

    @Test
    void programar_sinRevisoresConformes_falla() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS).revisoresConformes(false).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));

        assertThrows(BusinessException.class,
                () -> service.programarDefensa(TESIS, req("PRESENCIAL", LocalDate.of(2026, 9, 1), "Auditorio 3", null)));
        verify(proyectoRepository, never()).save(any());
    }

    @Test
    void programar_yaProgramada_falla() {
        ProyectoTesis p = proyectoConformes();
        p.setDefensaProgramada(true);

        assertThrows(BusinessException.class,
                () -> service.programarDefensa(TESIS, req("PRESENCIAL", LocalDate.of(2026, 9, 1), "Auditorio 3", null)));
    }

    @Test
    void programar_reprogramacionTrasDesaprobado_limpiaResultadoYRubricasAnteriores() {
        ProyectoTesis p = proyectoConformes();
        p.setDefensaRealizada(true);
        p.setResultadoDefensa(unmsm.edu.pe.tesis.domain.enums.ResultadoDefensa.DESAPROBADO);
        p.setFechaResultadoDefensa(LocalDate.of(2026, 8, 1));
        p.setObservacionDefensa("No sustenta el diseño");
        unmsm.edu.pe.tesis.domain.entities.RubricaDefensa rubricaVieja =
                unmsm.edu.pe.tesis.domain.entities.RubricaDefensa.builder()
                        .tesisId(TESIS).docenteId(DOC_A).storageKey("key-vieja").build();
        when(rubricaRepository.porTesis(TESIS)).thenReturn(List.of(rubricaVieja));

        service.programarDefensa(TESIS, req("PRESENCIAL", LocalDate.of(2026, 9, 1), "Auditorio 3", null));

        assertFalse(Boolean.TRUE.equals(p.getDefensaRealizada()));
        assertNull(p.getResultadoDefensa());
        assertNull(p.getFechaResultadoDefensa());
        assertNull(p.getObservacionDefensa());
        verify(almacenamiento).eliminar("key-vieja");
        verify(rubricaRepository).eliminarPorTesis(TESIS);
    }

    @Test
    void programar_proyectoInexistente_falla() {
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> service.programarDefensa(TESIS, req("PRESENCIAL", LocalDate.of(2026, 9, 1), "Auditorio 3", null)));
    }

    // ── defensa(): informa quién evalúa, sin jurado aparte ──

    @Test
    void defensa_listaLosRevisoresComoEvaluadores() {
        ProyectoTesis p = ProyectoTesis.builder().id(PROY).tesisId(TESIS)
                .defensaProgramada(true).fechaDefensa(LocalDate.of(2026, 9, 1)).build();
        when(proyectoRepository.buscarPorTesisId(TESIS)).thenReturn(Optional.of(p));

        ProyectoRevisor r1 = ProyectoRevisor.builder().docenteId(DOC_A).orden(1).build();
        ProyectoRevisor r2 = ProyectoRevisor.builder().docenteId(DOC_B).orden(2).build();
        when(revisorRepository.listarPorProyecto(PROY)).thenReturn(List.of(r1, r2));

        Persona pa = new Persona(); pa.setId(DOC_A); pa.setNombres("Ana"); pa.setApellidoPaterno("Reyes");
        Persona pb = new Persona(); pb.setId(DOC_B); pb.setNombres("Luis"); pb.setApellidoPaterno("Soto");
        when(personaRepository.buscarPorId(DOC_A)).thenReturn(Optional.of(pa));
        when(personaRepository.buscarPorId(DOC_B)).thenReturn(Optional.of(pb));

        var info = service.defensa(TESIS);

        assertTrue(info.isProgramada());
        assertEquals(2, info.getJurado().size());
        assertTrue(info.getJurado().stream().allMatch(j -> "REVISOR".equals(j.getRol())));
    }
}
