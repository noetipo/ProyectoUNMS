package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.ArchivoSubido;
import unmsm.edu.pe.personas.application.dto.DocumentoDescarga;
import unmsm.edu.pe.personas.application.dto.MiPerfilUpdateRequest;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoData;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoResponse;
import unmsm.edu.pe.personas.application.dto.PersonaResponse;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;

import java.util.Map;
import java.util.UUID;

public interface MiPerfilService {
    /** Perfil de la persona del usuario autenticado (del token). */
    PersonaResponse obtenerMiPerfil();

    /** Actualiza solo los campos seguros de la persona del usuario autenticado. */
    PersonaResponse actualizarMiPerfil(MiPerfilUpdateRequest request);

    /** Perfil completo (persona + historiales + documentos) del usuario autenticado. */
    PerfilCompletoResponse obtenerMiPerfilCompleto();

    /** Guarda historiales + documentos del usuario autenticado (persona del token). */
    PerfilCompletoResponse guardarMiPerfilCompleto(PerfilCompletoData data, Map<TipoDocumento, ArchivoSubido> archivos);

    /** Descarga un documento propio (persona del token); nunca el de otra persona. */
    DocumentoDescarga descargarMiDocumento(UUID documentoId);
}
