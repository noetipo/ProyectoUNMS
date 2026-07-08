package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.AgregarPerfilDocenteRequest;
import unmsm.edu.pe.personas.application.dto.ArchivoSubido;
import unmsm.edu.pe.personas.application.dto.CrearPersonaRequest;
import unmsm.edu.pe.personas.application.dto.PersonaListItem;
import unmsm.edu.pe.personas.application.dto.PersonaResponse;
import unmsm.edu.pe.personas.application.dto.PersonaUpdateRequest;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.shared.response.PageResponse;

import java.util.Map;
import java.util.UUID;

public interface PersonaService {

    /**
     * Registra una persona con su cuenta, perfil(es) y, en la misma operación,
     * sus historiales (cargos/centros) y documentos. Al crear, exige DNI y
     * PARTIDA_NACIMIENTO.
     */
    PersonaResponse registrar(CrearPersonaRequest request, Map<TipoDocumento, ArchivoSubido> archivos);

    /** Agrega el perfil docente a una persona que ya existe. */
    PersonaResponse agregarPerfilDocente(UUID personaId, AgregarPerfilDocenteRequest request);

    /** Ficha de la persona con perfiles y roles. */
    PersonaResponse obtener(UUID personaId);

    /** Listado paginado de personas con filtros. */
    PageResponse<PersonaListItem> listar(String search, String tipoPerfil, Boolean activo, int page, int size);

    /** Actualiza datos de la persona y de sus perfiles existentes. */
    PersonaResponse actualizar(UUID personaId, PersonaUpdateRequest request);

    /** Borrado lógico: marca la persona como inactiva. */
    void eliminarLogico(UUID personaId);
}
