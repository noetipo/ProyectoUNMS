package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Designación del Jurado Informante del informe final (3 docentes; el 1.º es Presidente). */
@Data
public class DesignarJuradoInformeRequest {
    private List<UUID> docenteIds;
}
