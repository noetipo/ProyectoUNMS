package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.ObservarItemRequest;
import unmsm.edu.pe.tesis.application.dto.ProyectoBandejaItem;
import unmsm.edu.pe.tesis.application.dto.ProyectoEditorResponse;

import java.util.List;
import java.util.UUID;

/** Casos de uso del ASESOR (docente) sobre los proyectos que asesora. */
public interface AsesorProyectoService {

    PageResponse<ProyectoBandejaItem> bandeja(String estado, String buscar, int page, int size);

    ProyectoEditorResponse detalle(UUID tesisId);

    /** El asesor observa un ítem del proyecto. */
    void observarItem(UUID tesisId, ObservarItemRequest req);

    /** El asesor da conformidad a un ítem corregido. */
    void darConformidad(UUID tesisId, String campo);

    /**
     * El asesor da conformidad a todos los ítems de una sección de una sola vez (flujo por bloque).
     * Omite los ítems con observación viva (OBSERVADO/EN_CORRECCION) para no pisarlas.
     */
    void darConformidadSeccion(UUID tesisId, List<String> campos);

    /** El asesor emite su carta de opinión favorable (exige todos los ítems conformes). */
    void emitirCartaOpinion(UUID tesisId);
}
