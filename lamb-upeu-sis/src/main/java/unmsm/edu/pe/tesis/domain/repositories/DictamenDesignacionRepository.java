package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.DictamenDesignacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DictamenDesignacionRepository {
    DictamenDesignacion save(DictamenDesignacion dictamen);
    Optional<DictamenDesignacion> buscarPorTesisId(UUID tesisId);
    /** Siguiente correlativo del año (atómico, seguro ante concurrencia). */
    int siguienteCorrelativo(int anio);

    /** Bandeja de secretaría: filas [tesisId, estudianteNombres, apPat, apMat, codigo, programaNombre,
     *  tituloTesis, estadoDictamen, numero]. Solo tesis con ambos firmados subidos. */
    List<Object[]> bandeja(String estado, UUID facultadId, UUID programaId, String buscar, int page, int size);
    long contarBandeja(String estado, UUID facultadId, UUID programaId, String buscar);
    /** Conteos por estado del dictamen (solo tesis con firmados completos). */
    long contarPorEstado(String estado);
}
