package unmsm.edu.pe.tesis.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/** Datos para generar los PDF de asesoría (Solicitud y Carta de aceptación). */
@Data
@Builder
public class DatosDocumentoAsesoria {
    private String estudianteNombre;
    private String estudianteCodigo;
    private String programaNombre;
    private String temaTitulo;
    private String lineaNombre;
    private String nivel;
    private String asesorNombre;
    private String asesorGrado;
    private LocalDate fecha; // solicitud: fecha de solicitud · carta: fecha de aceptación
}
