package unmsm.edu.pe.personas.domain.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unmsm.edu.pe.personas.application.dto.MiPerfilUpdateRequest;
import unmsm.edu.pe.personas.application.dto.PersonaResponse;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.services.PersonaService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MiPerfilServiceImplTest {

    @Mock SecurityUtils securityUtils;
    @Mock PersonaRepository personaRepository;
    @Mock PersonaService personaService;

    @InjectMocks MiPerfilServiceImpl service;

    private Persona personaConDatos(UUID id) {
        Persona p = Persona.builder()
                .id(id)
                .nombres("Juan")
                .apellidoPaterno("Perez")
                .numeroDocumento("12345678")
                .emailPersonal("viejo@gmail.com")
                .celular("111")
                .build();
        p.setActive(true);
        return p;
    }

    @Test
    void actualizar_soloCambiaCamposSeguros_yUsaPersonaDelToken() {
        UUID userId = UUID.randomUUID();
        UUID personaId = UUID.randomUUID();
        Persona persona = personaConDatos(personaId);

        when(securityUtils.getCurrentUserIdAsUUID()).thenReturn(userId);
        when(personaRepository.findByUserId(userId)).thenReturn(Optional.of(persona));
        when(personaService.obtener(personaId)).thenReturn(new PersonaResponse());

        MiPerfilUpdateRequest req = new MiPerfilUpdateRequest("nuevo@gmail.com", "999888777", "0000-0001-2345-6789");
        service.actualizarMiPerfil(req);

        // campos seguros actualizados
        assertEquals("nuevo@gmail.com", persona.getEmailPersonal());
        assertEquals("999888777", persona.getCelular());
        assertEquals("0000-0001-2345-6789", persona.getOrcid());
        // campos restringidos intactos
        assertEquals("12345678", persona.getNumeroDocumento());
        assertEquals("Perez", persona.getApellidoPaterno());
        verify(personaRepository).save(persona);
        // la persona vino del token, no de un id del cliente
        verify(personaRepository).findByUserId(userId);
    }

    @Test
    void obtener_sinPersonaAsociada_lanzaNotFound() {
        UUID userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserIdAsUUID()).thenReturn(userId);
        when(personaRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.obtenerMiPerfil());
        verify(personaService, never()).obtener(any());
    }
}
