package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.SeguimientoResponse;

import java.util.UUID;

/** Tablero de seguimiento de doctorandos (Secretaría): en qué etapa va cada uno y qué falta. */
public interface SeguimientoService {

    /**
     * @param programaId filtro opcional por programa
     * @param buscar     texto libre (nombres, apellidos, código, título)
     */
    SeguimientoResponse tablero(UUID programaId, String buscar);

    /**
     * El mismo tablero en Excel, con los filtros que el usuario tenga puestos en pantalla.
     *
     * @param etapa       etapa exacta (null = todas)
     * @param responsable responsable de la acción pendiente (null = todos)
     * @param programa    nombre del programa (el filtro de pantalla es por nombre, no por id)
     */
    byte[] excel(String buscar, Integer etapa, String responsable, String programa);
}
