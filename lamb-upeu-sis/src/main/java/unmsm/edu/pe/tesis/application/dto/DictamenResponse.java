package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictamenResponse {
    private UUID id;
    private UUID tesisId;
    private String numero;
    private String estado;
    private String expediente;
    private LocalDate fechaSolicitud;
    private LocalDate fechaEmision;
}
