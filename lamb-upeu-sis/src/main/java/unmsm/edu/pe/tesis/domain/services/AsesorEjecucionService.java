package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.EjecucionBandejaItem;
import unmsm.edu.pe.tesis.application.dto.EjecucionDetalleResponse;
import unmsm.edu.pe.tesis.application.dto.RegistrarAvanceRequest;

import java.util.List;
import java.util.UUID;

/** Casos de uso del ASESOR en la ejecución de la tesis (Etapa 6). */
public interface AsesorEjecucionService {

    /** Proyectos del asesor que están en ejecución (defensa del proyecto programada). */
    List<EjecucionBandejaItem> bandeja();

    /** Detalle de ejecución: plan, avances y estado del informe final. */
    EjecucionDetalleResponse detalle(UUID tesisId);

    /** Registra una evaluación de avance con la rúbrica de avances. */
    void registrarAvance(UUID tesisId, RegistrarAvanceRequest req);

    /** Aprueba el informe final (carta) — exige plan 100% ejecutado e informe subido. */
    void aprobarInformeFinal(UUID tesisId);
}
