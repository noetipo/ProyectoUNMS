package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.PlantillaRubrica;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantillaRubricaRepository {
    PlantillaRubrica save(PlantillaRubrica plantilla);
    Optional<PlantillaRubrica> buscarPorId(UUID id);

    /** La versión vigente del enfoque (CUANTITATIVO | CUALITATIVO), o vacío si aún no se publicó ninguna. */
    Optional<PlantillaRubrica> vigente(String enfoque);

    /** Todas las versiones del enfoque, de la más reciente a la más antigua. */
    List<PlantillaRubrica> historial(String enfoque);

    /** Quita la marca de vigente a las versiones anteriores del enfoque (queda solo la nueva). */
    void desmarcarVigentes(String enfoque);
}
