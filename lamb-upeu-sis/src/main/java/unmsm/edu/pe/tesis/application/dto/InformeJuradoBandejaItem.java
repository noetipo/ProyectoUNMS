package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Fila de la bandeja del Jurado Informante: un informe final asignado para evaluar. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformeJuradoBandejaItem {
    private UUID tesisId;
    private String estudianteApellidos;
    private String estudianteNombres;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private String miEstado;
    private boolean presidente;
}
