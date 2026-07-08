package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.Docente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocenteRepository {
    Docente save(Docente docente);
    Optional<Docente> findByPersonaId(UUID personaId);
    boolean existsByPersonaId(UUID personaId);
    boolean existsByCodigoSistema(String codigoSistema);
    boolean existsByEmailInstitucional(String emailInstitucional);

    /**
     * Listado paginado de docentes con su carga (asesorías + jurados vigentes),
     * calculada con subconsultas agregadas (sin N+1).
     * Cada fila: [persona_id, numero_documento, apellido_paterno, apellido_materno,
     * nombres, codigo_sistema, email_institucional, grado_academico, categoria,
     * condicion, active, asesorias_count, jurados_count].
     */
    List<Object[]> listar(String search, String grado, String categoria, String condicion, int page, int size);

    long contar(String search, String grado, String categoria, String condicion);

    /** Total de docentes con persona activa. */
    long contarActivos();

    /** Docentes activos con el grado académico dado. */
    long contarPorGrado(String grado);

    /** Docentes activos con al menos una asesoría vigente. */
    long contarAsesorando();

    /** Docentes activos con al menos una participación de jurado vigente. */
    long contarEnJurados();
}
