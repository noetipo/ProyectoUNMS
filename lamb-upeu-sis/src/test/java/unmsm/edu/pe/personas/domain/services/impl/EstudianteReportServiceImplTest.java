package unmsm.edu.pe.personas.domain.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.application.dto.EstudianteListItem;
import unmsm.edu.pe.personas.application.dto.EstudiantesResumen;
import unmsm.edu.pe.personas.application.mapper.PersonaMapper;
import unmsm.edu.pe.personas.domain.repositories.EstudianteRepository;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstudianteReportServiceImplTest {

    @Mock EstudianteRepository estudianteRepository;
    @Spy PersonaMapper mapper = new PersonaMapper();
    @InjectMocks EstudianteReportServiceImpl service;

    @Test
    void listar_mapeaFilas_aplicaFiltrosYPagina() {
        UUID personaId = UUID.randomUUID();
        UUID programaId = UUID.randomUUID();
        Object[] row = { personaId, "12345678", "Perez", "Lopez", "Juan",
                "E001", "M001", "j@unmsm.edu.pe", 2024, "REGULAR", "AUTOFINANCIADO",
                programaId, "DOCTORADO EN MEDICINA", "DOCTORADO", true };
        when(estudianteRepository.listar("ju", null, programaId, "REGULAR", "DOCTORADO", 1, 10))
                .thenReturn(List.<Object[]>of(row));
        when(estudianteRepository.contar("ju", null, programaId, "REGULAR", "DOCTORADO")).thenReturn(25L);

        PageResponse<EstudianteListItem> page =
                service.listar("ju", null, programaId, "REGULAR", "DOCTORADO", 1, 10);

        assertEquals(25, page.getTotal());
        assertEquals(1, page.getPage());
        assertEquals(10, page.getSize());
        EstudianteListItem item = page.getContent().get(0);
        assertEquals("M001", item.getCodMatricula());
        assertEquals("DOCTORADO EN MEDICINA", item.getProgramaNombre());
        assertEquals("REGULAR", item.getCondicion());
    }

    @Test
    void resumen_usaCountsLigeros() {
        when(estudianteRepository.contarActivos()).thenReturn(100L);
        when(estudianteRepository.contarPorCondicion("REGULAR")).thenReturn(70L);
        when(estudianteRepository.contarPorCondicion("EGRESADO")).thenReturn(20L);

        EstudiantesResumen r = service.resumen();

        assertEquals(100, r.getTotal());
        assertEquals(70, r.getRegulares());
        assertEquals(20, r.getEgresados());
        assertEquals(0, r.getConTesisActiva());
    }
}
