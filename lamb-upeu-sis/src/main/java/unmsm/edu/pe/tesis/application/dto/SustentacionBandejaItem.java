package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Fila de la bandeja de Sustentación: expedientes con Dictamen de Expedito, camino al acto final. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SustentacionBandejaItem {
    private UUID tesisId;
    private String estudianteNombre;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private LocalDate fechaSolicitud;
    private boolean expedienteRecibido;
    private int numJurado;
    private String estadoDictamen;
    private boolean sustentacionProgramada;
    private LocalDate fechaSustentacion;
    private boolean actaSubida;
    private boolean concluida;
    private String pendiente;
}
