package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.DocenteLineaRequest;
import unmsm.edu.pe.personas.domain.entities.Docente;

import java.util.List;

/**
 * Persiste las líneas de investigación de un docente dentro de la misma
 * transacción del guardado de persona (upsert idempotente, reemplazo total).
 */
public interface GuardarLineasInvestigacionService {

    /**
     * Reemplaza las líneas de investigación del docente por las indicadas.
     *
     * @param docente docente al que pertenecen las líneas (ya persistido)
     * @param lineas  lista de líneas; {@code null} no modifica nada (no provisto),
     *                lista vacía elimina todas las existentes.
     */
    void aplicar(Docente docente, List<DocenteLineaRequest> lineas);
}
