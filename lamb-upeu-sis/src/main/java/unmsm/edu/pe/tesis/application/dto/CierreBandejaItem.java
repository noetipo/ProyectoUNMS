package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Fila de la bandeja de cierre: proyectos con la defensa programada, pendientes de resultado o dictamen. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreBandejaItem {
    private UUID tesisId;
    private String estudianteNombre;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private LocalDate fechaDefensa;
    private String horaDefensa;
    private String modalidad;
    private String modalidadLabel;
    /** Rúbricas de defensa recibidas y cuántas se esperan (una por revisor). */
    private int rubricasRecibidas;
    private int rubricasEsperadas;
    private boolean defensaRealizada;
    private String resultado;
    private String resultadoLabel;
    private String estadoDictamen;       // POR_ELABORAR | ELABORADO | FIRMADO
    private String dictamenNumero;
    private boolean proyectoFinalArchivado;
    private boolean cerrado;
    /** Qué le toca hacer ahora a la Secretaría. */
    private String pendiente;
}
