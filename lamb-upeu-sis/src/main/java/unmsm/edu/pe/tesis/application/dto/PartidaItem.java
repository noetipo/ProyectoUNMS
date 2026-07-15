package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** Partida del presupuesto por rubro. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartidaItem {
    private UUID id;
    private String rubro;
    private String descripcion;
    private BigDecimal monto;
    private Integer orden;
}
