package unmsm.edu.pe.configuracion.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LemaAnualResponse {
    private UUID id;
    private Integer anio;
    private String texto;
    private Boolean activo;
}
