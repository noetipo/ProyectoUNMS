package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.math.BigDecimal;

/** Alta/edición de una partida del presupuesto. */
@Data
public class PartidaRequest {
    private String rubro;
    private String descripcion;
    private BigDecimal monto;
    private Integer orden;
}
