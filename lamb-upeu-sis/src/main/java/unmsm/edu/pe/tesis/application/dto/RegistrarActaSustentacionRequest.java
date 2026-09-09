package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

/** Resultado del acto de sustentación que registra la Secretaría junto con el Acta firmada. */
@Data
public class RegistrarActaSustentacionRequest {
    /** APROBADO | APROBADO_CON_OBSERVACIONES | DESAPROBADO */
    private String resultado;
    private String observacion;
}
