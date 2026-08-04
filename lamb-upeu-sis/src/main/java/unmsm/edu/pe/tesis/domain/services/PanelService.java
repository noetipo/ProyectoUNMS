package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.PanelResponse;

/** Panel de inicio: lo importante primero según el rol + cifras del programa para todos. */
public interface PanelService {

    PanelResponse panel();

    /** El mismo panel en Excel (cifras del programa + lo pendiente de quien lo descarga). */
    byte[] reporteExcel();
}
