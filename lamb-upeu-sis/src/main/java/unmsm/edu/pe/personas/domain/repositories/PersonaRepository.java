package unmsm.edu.pe.personas.domain.repositories;

import unmsm.edu.pe.personas.domain.entities.Persona;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonaRepository {
    Persona save(Persona persona);
    Optional<Persona> buscarPorId(UUID id);
    Optional<Persona> findByUserId(UUID userId);
    boolean existsByNumeroDocumento(String numeroDocumento);
    boolean existsByOrcid(String orcid);

    /**
     * Listado paginado de personas.
     * Cada fila: [id, numero_documento, apellido_paterno, apellido_materno,
     * nombres, email_personal, celular, active, has_estudiante, has_docente].
     */
    List<Object[]> listar(String search, String tipoPerfil, Boolean activo, int page, int size);

    long contar(String search, String tipoPerfil, Boolean activo);
}
