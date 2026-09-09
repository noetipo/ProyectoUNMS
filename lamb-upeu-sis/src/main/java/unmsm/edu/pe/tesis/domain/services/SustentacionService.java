package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.ProgramarSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.RegistrarActaSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.SustentacionBandejaItem;
import unmsm.edu.pe.tesis.application.dto.SustentacionTramiteResponse;

import java.util.List;
import java.util.UUID;

/**
 * Trámite de Sustentación (Etapa 8, la última): recepción de la solicitud, dictamen de
 * designación del Jurado, programación del acto, Acta de sustentación y archivo final, que
 * concluye el proceso de titulación.
 */
public interface SustentacionService {

    List<SustentacionBandejaItem> bandeja(String buscar);

    SustentacionTramiteResponse detalle(UUID tesisId);

    /** La Secretaría recibe el expediente y comunica al Coordinador (para que designe el jurado). */
    void recepcionar(UUID tesisId);

    void elaborarDictamen(UUID tesisId, ElaborarDictamenSustentacionRequest req);

    byte[] documentoDictamen(UUID tesisId, String formato);

    void subirDictamenFirmado(UUID tesisId, byte[] contenido, String nombreOriginal, String contentType);

    void programar(UUID tesisId, ProgramarSustentacionRequest req);

    /** Registra el resultado del acto y archiva el Acta firmada; concluye el proceso de titulación. */
    void registrarActa(UUID tesisId, RegistrarActaSustentacionRequest req, byte[] contenido,
                       String nombreOriginal, String contentType);
}
