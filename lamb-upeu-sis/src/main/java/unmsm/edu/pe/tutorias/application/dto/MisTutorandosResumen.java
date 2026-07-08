package unmsm.edu.pe.tutorias.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cabecera del panel del tutor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisTutorandosResumen {
    private String tutorNombre;
    private long total;
    private int cupoMaximo;
}
