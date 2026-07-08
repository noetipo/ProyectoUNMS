package unmsm.edu.pe.personas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocentesResumen {
    private long total;
    private long doctores;
    private long asesorando;
    private long enJurados;
}
