package unmsm.edu.pe.personas.domain.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.application.dto.DocenteLineaRequest;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardarLineasInvestigacionServiceImplTest {

    @Mock LineaInvestigacionRepository lineaInvestigacionRepository;
    @Mock DocenteLineaInvestigacionRepository docenteLineaRepository;

    @InjectMocks GuardarLineasInvestigacionServiceImpl service;

    private Docente docente;
    private UUID docenteId;

    @BeforeEach
    void setUp() {
        docenteId = UUID.randomUUID();
        docente = Docente.builder().personaId(docenteId).build();
    }

    private LineaInvestigacion lineaActiva(UUID id) {
        LineaInvestigacion l = LineaInvestigacion.builder().id(id).nombre("IA").build();
        l.setActive(true);
        return l;
    }

    @Test
    void aplicar_feliz_reemplazaYGuarda() {
        UUID l1 = UUID.randomUUID();
        UUID l2 = UUID.randomUUID();
        when(lineaInvestigacionRepository.buscarPorId(l1)).thenReturn(Optional.of(lineaActiva(l1)));
        when(lineaInvestigacionRepository.buscarPorId(l2)).thenReturn(Optional.of(lineaActiva(l2)));

        service.aplicar(docente, List.of(
                new DocenteLineaRequest(l1, true),
                new DocenteLineaRequest(l2, false)));

        verify(docenteLineaRepository).deleteByDocenteId(docenteId);
        verify(docenteLineaRepository).saveAll(argThat(list -> list.size() == 2));
    }

    @Test
    void aplicar_null_noHaceNada() {
        service.aplicar(docente, null);
        verify(docenteLineaRepository, never()).deleteByDocenteId(any());
        verify(docenteLineaRepository, never()).saveAll(any());
    }

    @Test
    void aplicar_listaVacia_eliminaTodasYNoInserta() {
        service.aplicar(docente, List.of());
        verify(docenteLineaRepository).deleteByDocenteId(docenteId);
        verify(docenteLineaRepository, never()).saveAll(any());
    }

    @Test
    void aplicar_dosPrincipales_lanzaValidation() {
        assertThrows(ValidationException.class, () -> service.aplicar(docente, List.of(
                new DocenteLineaRequest(UUID.randomUUID(), true),
                new DocenteLineaRequest(UUID.randomUUID(), true))));
        verify(docenteLineaRepository, never()).saveAll(any());
    }

    @Test
    void aplicar_lineaDuplicada_lanzaValidation() {
        UUID l1 = UUID.randomUUID();
        assertThrows(ValidationException.class, () -> service.aplicar(docente, List.of(
                new DocenteLineaRequest(l1, false),
                new DocenteLineaRequest(l1, false))));
        verify(docenteLineaRepository, never()).saveAll(any());
    }

    @Test
    void aplicar_lineaInexistenteOInactiva_lanzaBusiness() {
        UUID l1 = UUID.randomUUID();
        when(lineaInvestigacionRepository.buscarPorId(l1)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> service.aplicar(docente, List.of(new DocenteLineaRequest(l1, false))));
    }
}