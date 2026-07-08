package unmsm.edu.pe.personas.domain.services;

import unmsm.edu.pe.personas.application.dto.GradoAcademicoRequest;
import unmsm.edu.pe.personas.domain.entities.Persona;

import java.util.List;

/**
 * Persiste los grados académicos de una persona dentro de la misma transacción del
 * guardado de persona (reemplazo total idempotente, valida un solo principal).
 * Réplica de {@code GuardarLineasInvestigacionService}.
 */
public interface GuardarGradosAcademicosService {

    /**
     * @param persona persona ya persistida
     * @param grados  {@code null} = no se modifica; lista (posiblemente vacía) = reemplaza los existentes
     */
    void aplicar(Persona persona, List<GradoAcademicoRequest> grados);
}
