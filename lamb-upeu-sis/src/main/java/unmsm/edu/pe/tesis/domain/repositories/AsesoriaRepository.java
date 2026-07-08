package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.Asesoria;

import java.util.Optional;
import java.util.UUID;

public interface AsesoriaRepository {
    Asesoria save(Asesoria asesoria);

    /** ¿La tesis ya tiene un asesor (asesoría tipo ASESOR activa)? */
    boolean existeAsesorParaTesis(UUID tesisId);

    /** Asesoría activa de una tesis por tipo (ASESOR | COASESOR). */
    Optional<Asesoria> buscarPorTesisYTipo(UUID tesisId, String tipo);

    /** Carga: nº de asesorías (tipo ASESOR activas) de un docente. */
    long contarAsesoriasDeDocente(UUID docenteId);
}
