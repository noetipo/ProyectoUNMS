package unmsm.edu.pe.tesis.application.dto;

import lombok.Builder;
import lombok.Data;

/** Tarjetas de resumen del reporte del coordinador. */
@Data
@Builder
public class EstudiantesTemaResumen {
    private long total;
    private long conTema;
    private long sinTema;
    /** Con tema registrado pero sin tutor designado: el paso pendiente que sigue al tema. */
    private long sinTutor;
}
