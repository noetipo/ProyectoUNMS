package unmsm.edu.pe.personas.domain.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.application.dto.PersonaListItem;
import unmsm.edu.pe.personas.application.mapper.PersonaMapper;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.*;
import unmsm.edu.pe.security.domain.repositories.RoleRepository;
import unmsm.edu.pe.security.domain.repositories.UserRepository;
import unmsm.edu.pe.security.domain.repositories.UserRoleAssignmentRepository;
import unmsm.edu.pe.security.infrastructure.utils.PasswordEncoder;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonaCrudServiceImplTest {

    @Mock PersonaRepository personaRepository;
    @Mock EstudianteRepository estudianteRepository;
    @Mock DocenteRepository docenteRepository;
    @Mock ProgramaPosgradoRepository programaRepository;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleAssignmentRepository userRoleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Spy PersonaMapper mapper = new PersonaMapper();

    @InjectMocks PersonaServiceImpl service;

    @Test
    void listar_mapeaFilasYTotal_yAplicaFiltros() {
        UUID id = UUID.randomUUID();
        Object[] row = { id, "12345678", "Perez", "Lopez", "Juan",
                "juan@gmail.com", "999", true, true, false };
        when(personaRepository.listar("juan", "ESTUDIANTE", true, 0, 20))
                .thenReturn(List.<Object[]>of(row));
        when(personaRepository.contar("juan", "ESTUDIANTE", true)).thenReturn(1L);

        PageResponse<PersonaListItem> page = service.listar("juan", "ESTUDIANTE", true, 0, 20);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getContent().size());
        PersonaListItem item = page.getContent().get(0);
        assertEquals("12345678", item.getNumeroDocumento());
        assertEquals("Perez Lopez", item.getApellidos());
        assertTrue(item.getPerfiles().contains("ESTUDIANTE"));
        assertFalse(item.getPerfiles().contains("DOCENTE"));
        // verifica que pasó los filtros al repositorio
        verify(personaRepository).listar("juan", "ESTUDIANTE", true, 0, 20);
    }

    @Test
    void eliminarLogico_marcaInactivoYGuarda() {
        UUID id = UUID.randomUUID();
        Persona persona = Persona.builder().id(id).nombres("Juan").build();
        persona.setActive(true);
        when(personaRepository.buscarPorId(id)).thenReturn(Optional.of(persona));

        service.eliminarLogico(id);

        assertFalse(persona.getActive());
        verify(personaRepository).save(persona);
    }

    @Test
    void eliminarLogico_personaNoExiste_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(personaRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.eliminarLogico(id));
        verify(personaRepository, never()).save(any());
    }
}
