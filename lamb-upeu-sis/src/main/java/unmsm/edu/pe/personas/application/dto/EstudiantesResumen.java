package unmsm.edu.pe.personas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstudiantesResumen {
    private long total;
    private long regulares;
    private long conTesisActiva;
    private long egresados;
}
