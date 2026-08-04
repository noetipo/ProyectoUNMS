package unmsm.edu.pe.tesis.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.tesis.domain.entities.Asesoria;
import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;
import unmsm.edu.pe.tesis.domain.repositories.AsesoriaRepository;
import unmsm.edu.pe.tesis.domain.repositories.ProyectoTesisRepository;

import java.util.UUID;

/**
 * Resuelve quién es el asesor vigente de una tesis. La <b>fuente de la verdad</b> es la tabla
 * {@code asesorias}: {@code proyectos_tesis.asesor_id} es solo una copia escrita al crear el
 * proyecto, y queda desactualizada cuando el alumno abre el editor antes de que su asesor acepte
 * (o cuando la designación cambia). Por eso todo consumidor debe pasar por aquí, y de paso se
 * repara la copia.
 */
@ApplicationScoped
public class AsesorDesignadoService {

    @Inject AsesoriaRepository asesoriaRepository;
    @Inject ProyectoTesisRepository proyectoRepository;
    @Inject AsesorRolService asesorRolService;

    /** Asesor principal designado en la tesis, o null si aún no lo tiene. */
    public UUID asesorId(UUID tesisId) {
        if (tesisId == null) return null;
        return asesoriaRepository.buscarPorTesisYTipo(tesisId, "ASESOR")
                .map(Asesoria::getDocenteId).orElse(null);
    }

    /** ¿Ese docente es el co-asesor de la tesis? (acceso de solo consulta) */
    public boolean esCoasesor(UUID tesisId, UUID docenteId) {
        if (tesisId == null || docenteId == null) return false;
        return asesoriaRepository.buscarPorTesisYTipo(tesisId, "COASESOR")
                .map(a -> docenteId.equals(a.getDocenteId())).orElse(false);
    }

    /**
     * Devuelve el asesor vigente del proyecto y, si la copia {@code asesor_id} está vacía o
     * desactualizada, la corrige (y asegura el rol ASESOR del docente).
     */
    public UUID sincronizar(ProyectoTesis p) {
        if (p == null) return null;
        UUID vigente = asesorId(p.getTesisId());
        if (vigente != null && !vigente.equals(p.getAsesorId())) {
            p.setAsesorId(vigente);
            proyectoRepository.save(p);
            asesorRolService.otorgarRolAsesor(vigente);
        }
        return vigente;
    }
}
