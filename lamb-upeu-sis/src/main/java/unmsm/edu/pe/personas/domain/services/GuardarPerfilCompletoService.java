package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.ArchivoSubido;
import unmsm.edu.pe.personas.application.dto.DocumentoDescarga;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoData;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoResponse;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;

import java.util.Map;
import java.util.UUID;

public interface GuardarPerfilCompletoService {

    /**
     * Aplica historiales + documentos sobre una persona ya cargada, DENTRO de una
     * transacción existente (usado por el registro). Si falla, limpia los archivos
     * recién subidos (best-effort) y propaga la excepción para hacer rollback.
     */
    void aplicar(Persona persona, PerfilCompletoData data,
                 Map<TipoDocumento, ArchivoSubido> archivos, boolean validarRequeridos);

    /** Carga la persona por id y aplica el perfil completo (endpoint de terceros). */
    PerfilCompletoResponse guardar(UUID personaId, PerfilCompletoData data,
                                   Map<TipoDocumento, ArchivoSubido> archivos, boolean validarRequeridos);

    /** Ficha completa: persona + historiales + documentos. */
    PerfilCompletoResponse obtenerPerfilCompleto(UUID personaId);

    /**
     * Recupera el contenido de un documento para servirlo. Verifica que el
     * documento pertenezca a {@code personaId} (evita IDOR): si no, se trata como
     * no encontrado.
     */
    DocumentoDescarga descargarDocumento(UUID personaId, UUID documentoId);
}
