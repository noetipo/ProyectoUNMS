package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vista del Jurado Informante: datos de la tesis + su evaluación del informe final. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformeEvaluacionResponse {
    private String estudianteNombre;
    private String codigoSistema;
    private String programaNombre;
    private String titulo;
    private boolean informeFinalSubido;
    private boolean presidente;
    private String miEstado;
    private Integer miPuntaje;
    private String miComentario;
    private String respuestaEstudiante;
    private boolean cerrada;
    private int puntajeMaximo;
}
