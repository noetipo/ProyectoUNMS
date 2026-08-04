package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.PlantillaRubrica;
import unmsm.edu.pe.tesis.domain.repositories.PlantillaRubricaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PlantillaRubricaRepositoryImpl
        implements PlantillaRubricaRepository, PanacheRepositoryBase<PlantillaRubrica, UUID> {

    @Override
    public PlantillaRubrica save(PlantillaRubrica plantilla) {
        if (plantilla.getId() == null) {
            persist(plantilla);
            return plantilla;
        }
        return getEntityManager().merge(plantilla);
    }

    @Override
    public Optional<PlantillaRubrica> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public Optional<PlantillaRubrica> vigente(String enfoque) {
        return find("enfoque = ?1 and vigente = true and active = true", enfoque).firstResultOptional();
    }

    @Override
    public List<PlantillaRubrica> historial(String enfoque) {
        return list("enfoque = ?1 and active = true order by fechaCarga desc", enfoque);
    }

    @Override
    public void desmarcarVigentes(String enfoque) {
        update("vigente = false where enfoque = ?1 and vigente = true", enfoque);
    }
}
