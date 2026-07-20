package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Fila de la bandeja del revisor (Etapa 5): un proyecto asignado para evaluar. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisorBandejaItem {
    private UUID tesisId;
    private UUID proyectoId;
    private String estudianteApellidos;
    private String estudianteNombres;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private LocalDate fechaRecepcion;
    private String miEstado;       // EstadoRevisor del docente actual
    private Integer miPuntaje;     // puntaje total dado (si evaluó)
}
