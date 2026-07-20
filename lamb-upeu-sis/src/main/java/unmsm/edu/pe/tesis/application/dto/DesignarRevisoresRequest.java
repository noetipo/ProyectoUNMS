package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Designación de los revisores del proyecto (normalmente 2 docentes). */
@Data
public class DesignarRevisoresRequest {
    private List<UUID> docenteIds;
}
