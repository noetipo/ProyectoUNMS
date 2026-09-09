package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Rúbrica de defensa de un revisor: si ya se recepcionó, con qué nota y en qué archivo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricaDefensaItem {
    private UUID docenteId;
    private String docenteNombre;
    private boolean recibida;
    private Integer puntaje;
    private String nombreArchivo;
    private LocalDate fechaCarga;
}
