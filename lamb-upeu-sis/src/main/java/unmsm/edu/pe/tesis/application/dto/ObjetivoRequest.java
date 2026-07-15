package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

/** Alta/edición de un objetivo específico. */
@Data
public class ObjetivoRequest {
    private String texto;
    private Integer orden;
}
