package unmsm.edu.pe.personas.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import unmsm.edu.pe.personas.application.dto.ArchivoSubido;
import unmsm.edu.pe.personas.application.dto.DocumentoDescarga;
import unmsm.edu.pe.personas.application.dto.MiPerfilUpdateRequest;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoData;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoResponse;
import unmsm.edu.pe.personas.application.dto.PersonaResponse;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.personas.domain.repositories.PersonaRepository;
import unmsm.edu.pe.personas.domain.services.GuardarPerfilCompletoService;
import unmsm.edu.pe.personas.domain.services.MiPerfilService;
import unmsm.edu.pe.personas.domain.services.PersonaService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class MiPerfilServiceImpl implements MiPerfilService {

    @Inject SecurityUtils securityUtils;
    @Inject PersonaRepository personaRepository;
    @Inject PersonaService personaService;
    @Inject GuardarPerfilCompletoService perfilCompletoService;

    @Override
    public PersonaResponse obtenerMiPerfil() {
        Persona persona = personaActual();
        return personaService.obtener(persona.getId());
    }

    @Override
    @Transactional
    public PersonaResponse actualizarMiPerfil(MiPerfilUpdateRequest request) {
        Persona persona = personaActual();

        // Solo campos seguros: email_personal, celular, orcid
        if (request.getEmailPersonal() != null) {
            persona.setEmailPersonal(request.getEmailPersonal());
        }
        if (request.getCelular() != null) {
            persona.setCelular(request.getCelular());
        }
        if (request.getOrcid() != null) {
            String orcid = request.getOrcid().isBlank() ? null : request.getOrcid().trim();
            if (orcid != null && !orcid.equals(persona.getOrcid()) && personaRepository.existsByOrcid(orcid)) {
                throw new BusinessException("El ORCID ya está registrado por otra persona");
            }
            persona.setOrcid(orcid);
        }
        personaRepository.save(persona);

        return personaService.obtener(persona.getId());
    }

    @Override
    public PerfilCompletoResponse obtenerMiPerfilCompleto() {
        return perfilCompletoService.obtenerPerfilCompleto(personaActual().getId());
    }

    @Override
    public PerfilCompletoResponse guardarMiPerfilCompleto(PerfilCompletoData data, Map<TipoDocumento, ArchivoSubido> archivos) {
        // La persona se obtiene del token, nunca de un id del cliente.
        return perfilCompletoService.guardar(personaActual().getId(), data, archivos, true);
    }

    @Override
    public DocumentoDescarga descargarMiDocumento(UUID documentoId) {
        // El documento se resuelve contra la persona del token: impide acceder al de otra persona.
        return perfilCompletoService.descargarDocumento(personaActual().getId(), documentoId);
    }

    /** Persona ligada al usuario autenticado (id del token). Nunca de un id del cliente. */
    private Persona personaActual() {
        UUID userId = securityUtils.getCurrentUserIdAsUUID();
        if (userId == null) {
            throw new BusinessException("Usuario no autenticado");
        }
        return personaRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("El usuario autenticado no tiene una persona asociada"));
    }
}
