package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteResumen {
    private long tutores;
    private long estudiantesConTutor;
    private long estudiantesSinTutor;
    private long promedioPorTutor;
}
