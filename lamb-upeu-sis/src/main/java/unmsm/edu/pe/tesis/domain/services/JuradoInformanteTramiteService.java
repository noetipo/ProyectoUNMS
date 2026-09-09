package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenExpeditoRequest;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenJuradoInformeRequest;
import unmsm.edu.pe.tesis.application.dto.JuradoInformeBandejaItem;
import unmsm.edu.pe.tesis.application.dto.JuradoInformeTramiteResponse;

import java.util.List;
import java.util.UUID;

/**
 * Trámite del Jurado Informante (Etapa 7) que corresponde a la Secretaría: recepcionar la
 * solicitud del estudiante, elaborar y subir el dictamen de designación, archivar el expediente
 * cuando el Jurado da su conformidad, y elaborar/subir el Dictamen de Expedito que habilita al
 * doctorando a solicitar su sustentación.
 */
public interface JuradoInformanteTramiteService {

    List<JuradoInformeBandejaItem> bandeja(String buscar);

    JuradoInformeTramiteResponse detalle(UUID tesisId);

    /** La Secretaría recibe el expediente y comunica al Coordinador (para que designe el jurado). */
    void recepcionar(UUID tesisId);

    void elaborarDictamen(UUID tesisId, ElaborarDictamenJuradoInformeRequest req);

    byte[] documentoDictamen(UUID tesisId, String formato);

    void subirDictamenFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType);

    /** Archiva el expediente del Jurado Informante una vez que los 3 miembros dan conformidad. */
    void archivarExpediente(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType);

    void elaborarDictamenExpedito(UUID tesisId, ElaborarDictamenExpeditoRequest req);

    byte[] documentoExpedito(UUID tesisId, String formato);

    void subirDictamenExpeditoFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType);
}
