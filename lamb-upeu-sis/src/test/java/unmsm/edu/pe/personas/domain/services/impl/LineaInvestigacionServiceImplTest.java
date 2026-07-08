package unmsm.edu.pe.personas.domain.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.application.dto.LineaInvestigacionRequest;
import unmsm.edu.pe.personas.application.dto.LineaInvestigacionResponse;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.repositories.LineaInvestigacionRepository;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LineaInvestigacionServiceImplTest {

    @Mock LineaInvestigacionRepository lineaInvestigacionRepository;

    @InjectMocks LineaInvestigacionServiceImpl service;

    private LineaInvestigacion linea(UUID id, String nombre) {
        LineaInvestigacion l = LineaInvestigacion.builder()
                .id(id).codigo("LIN-001").nombre(nombre).descripcion("desc").build();
        l.setActive(true);
        return l;
    }

    @Test
    void listar_devuelvePaginaMapeada() {
        UUID id = UUID.randomUUID();
        when(lineaInvestigacionRepository.listar("ia", 0, 20))
                .thenReturn(List.of(linea(id, "Inteligencia Artificial")));
        when(lineaInvestigacionRepository.contar("ia")).thenReturn(1L);

        PageResponse<LineaInvestigacionResponse> page = service.listar("ia", 0, 20);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getContent().size());
        LineaInvestigacionResponse r = page.getContent().get(0);
        assertEquals(id, r.getId());
        assertEquals("Inteligencia Artificial", r.getNombre());
        assertTrue(r.getActivo());
    }

    @Test
    void obtener_existente_devuelveResponse() {
        UUID id = UUID.randomUUID();
        when(lineaInvestigacionRepository.buscarPorId(id))
                .thenReturn(Optional.of(linea(id, "Ciencia de Datos")));

        LineaInvestigacionResponse r = service.obtener(id);

        assertEquals("Ciencia de Datos", r.getNombre());
        assertEquals("LIN-001", r.getCodigoSistema());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(lineaInvestigacionRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.obtener(id));
    }

    // ── crear ──

    @Test
    void crear_nombreUnico_generaCodigoYGuarda() {
        when(lineaInvestigacionRepository.existsByNombre("Robótica Médica")).thenReturn(false);
        when(lineaInvestigacionRepository.contarTotal()).thenReturn(4L);
        when(lineaInvestigacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LineaInvestigacionResponse r = service.crear(new LineaInvestigacionRequest("Robótica Médica", "desc"));

        assertEquals("Robótica Médica", r.getNombre());
        assertEquals("LIN-00005", r.getCodigoSistema());
        verify(lineaInvestigacionRepository).save(any(LineaInvestigacion.class));
    }

    @Test
    void crear_nombreDuplicado_lanzaBusiness() {
        when(lineaInvestigacionRepository.existsByNombre("Oncología")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.crear(new LineaInvestigacionRequest("Oncología", null)));
        verify(lineaInvestigacionRepository, never()).save(any());
    }

    // ── actualizar ──

    @Test
    void actualizar_existente_guardaCambios() {
        UUID id = UUID.randomUUID();
        LineaInvestigacion existente = linea(id, "Nombre Viejo");
        when(lineaInvestigacionRepository.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(lineaInvestigacionRepository.existsByNombre("Nombre Nuevo")).thenReturn(false);
        when(lineaInvestigacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LineaInvestigacionResponse r = service.actualizar(id, new LineaInvestigacionRequest("Nombre Nuevo", "d2"));

        assertEquals("Nombre Nuevo", r.getNombre());
        assertEquals("d2", r.getDescripcion());
    }

    @Test
    void actualizar_nombreDuplicado_lanzaBusiness() {
        UUID id = UUID.randomUUID();
        when(lineaInvestigacionRepository.buscarPorId(id)).thenReturn(Optional.of(linea(id, "Actual")));
        when(lineaInvestigacionRepository.existsByNombre("Ocupado")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.actualizar(id, new LineaInvestigacionRequest("Ocupado", null)));
        verify(lineaInvestigacionRepository, never()).save(any());
    }

    @Test
    void actualizar_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(lineaInvestigacionRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.actualizar(id, new LineaInvestigacionRequest("X", null)));
    }

    // ── eliminar ──

    @Test
    void eliminar_existente_desactiva() {
        UUID id = UUID.randomUUID();
        LineaInvestigacion existente = linea(id, "A eliminar");
        when(lineaInvestigacionRepository.buscarPorId(id)).thenReturn(Optional.of(existente));

        service.eliminar(id);

        assertFalse(existente.getActive());
        verify(lineaInvestigacionRepository).save(existente);
    }

    @Test
    void eliminar_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(lineaInvestigacionRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.eliminar(id));
        verify(lineaInvestigacionRepository, never()).save(any());
    }
}