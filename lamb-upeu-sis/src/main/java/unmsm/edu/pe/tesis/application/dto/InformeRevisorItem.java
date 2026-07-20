package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Miembro del Jurado Informante y su evaluación del informe final. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformeRevisorItem {
    private UUID id;
    private UUID docenteId;
    private String docenteNombre;
    private Integer orden;
    private boolean presidente;
    private String estado;       // DESIGNADO | OBSERVADO | CONFORME
    private Integer puntaje;
    private String comentario;
    private String respuesta;    // levantamiento del estudiante
}
