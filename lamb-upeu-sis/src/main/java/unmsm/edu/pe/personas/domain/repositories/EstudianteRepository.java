package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.Estudiante;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstudianteRepository {
    Estudiante save(Estudiante estudiante);
    Optional<Estudiante> findByPersonaId(UUID personaId);
    boolean existsByPersonaId(UUID personaId);
    boolean existsByCodigoSistema(String codigoSistema);
    boolean existsByCodMatricula(String codMatricula);
    boolean existsByEmailInstitucional(String emailInstitucional);

    /**
     * Listado paginado de estudiantes (solo persona activa).
     * Cada fila: [persona_id, numero_documento, apellido_paterno, apellido_materno,
     * nombres, codigo_sistema, cod_matricula, email_institucional, anio_ingreso,
     * condicion, financiamiento, programa_id, programa_nombre, programa_nivel, active].
     */
    List<Object[]> listar(String search, UUID facultadId, UUID programaId, String condicion, String nivel, int page, int size);

    long contar(String search, UUID facultadId, UUID programaId, String condicion, String nivel);

    /** Total de estudiantes con persona activa. */
    long contarActivos();

    /** Estudiantes activos con la condición dada. */
    long contarPorCondicion(String condicion);
}
