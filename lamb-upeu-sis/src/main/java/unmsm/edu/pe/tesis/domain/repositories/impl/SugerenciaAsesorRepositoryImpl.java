package unmsm.edu.pe.tesis.domain.repositories.impl;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.tesis.domain.entities.SugerenciaAsesor;
import unmsm.edu.pe.tesis.domain.repositories.SugerenciaAsesorRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SugerenciaAsesorRepositoryImpl
        implements SugerenciaAsesorRepository, PanacheRepositoryBase<SugerenciaAsesor, UUID> {

    @Override
    public SugerenciaAsesor save(SugerenciaAsesor sugerencia) {
        if (sugerencia.getId() == null) {
            persist(sugerencia);
            return sugerencia;
        }
        return getEntityManager().merge(sugerencia);
    }

    @Override
    public Optional<SugerenciaAsesor> buscarPorId(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public List<SugerenciaAsesor> listarPorEstudiante(UUID estudianteId) {
        return list("estudianteId = ?1 and active = true order by createdAt", estudianteId);
    }

    @Override
    public boolean existe(UUID estudianteId, UUID asesorDocenteId) {
        return count("estudianteId = ?1 and asesorDocenteId = ?2 and active = true",
                estudianteId, asesorDocenteId) > 0;
    }
}
