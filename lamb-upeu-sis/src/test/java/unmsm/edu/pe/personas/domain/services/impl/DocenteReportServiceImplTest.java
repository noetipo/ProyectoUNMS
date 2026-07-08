package unmsm.edu.pe.personas.domain.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.application.dto.DocenteListItem;
import unmsm.edu.pe.personas.application.dto.DocentesResumen;
import unmsm.edu.pe.personas.application.mapper.PersonaMapper;
import unmsm.edu.pe.personas.domain.repositories.DocenteRepository;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocenteReportServiceImplTest {

    @Mock DocenteRepository docenteRepository;
    @Spy PersonaMapper mapper = new PersonaMapper();
    @InjectMocks DocenteReportServiceImpl service;

    @Test
    void listar_calculaCargaComoAsesoriasMasJurados() {
        UUID personaId = UUID.randomUUID();
        Object[] row = { personaId, "12345678", "Perez", "Lopez", "Ana",
                "D001", "ana@unmsm.edu.pe", "DOCTOR", "PRINCIPAL", "NOMBRADO",
                true, 3L, 2L };
        when(docenteRepository.listar("an", "DOCTOR", null, null, 0, 20))
                .thenReturn(List.<Object[]>of(row));
        when(docenteRepository.contar("an", "DOCTOR", null, null)).thenReturn(1L);

        PageResponse<DocenteListItem> page = service.listar("an", "DOCTOR", null, null, 0, 20);

        DocenteListItem item = page.getContent().get(0);
        assertEquals(3, item.getAsesorias());
        assertEquals(2, item.getJurados());
        assertEquals(5, item.getCarga());
        assertEquals("DOCTOR", item.getGradoAcademico());
    }

    @Test
    void resumen_cuentaTotalesYCarga() {
        when(docenteRepository.contarActivos()).thenReturn(40L);
        when(docenteRepository.contarPorGrado("DOCTOR")).thenReturn(25L);
        when(docenteRepository.contarAsesorando()).thenReturn(12L);
        when(docenteRepository.contarEnJurados()).thenReturn(8L);

        DocentesResumen r = service.resumen();

        assertEquals(40, r.getTotal());
        assertEquals(25, r.getDoctores());
        assertEquals(12, r.getAsesorando());
        assertEquals(8, r.getEnJurados());
    }
}
