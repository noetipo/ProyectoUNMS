package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/** Fila de la bandeja del coordinador (Etapa 5): proyecto recepcionado, pendiente/con revisores. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefensaBandejaItem {
    private UUID tesisId;
    private UUID proyectoId;
    private String estudianteApellidos;
    private String estudianteNombres;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private LocalDate fechaRecepcion;
    private int numRevisores;
    private boolean revisoresDesignados; // numRevisores >= 2
    private boolean revisoresConformes;  // ambos revisores dieron conformidad
    private boolean defensaProgramada;   // ya se programó la defensa
    private LocalDate fechaDefensa;
    // Etapa 7 · Jurado Informante del informe final
    private boolean juradoInformanteSolicitado;
    private int numJuradoInforme;        // miembros del Jurado Informante designados
    private boolean juradoInformeDesignado; // numJuradoInforme >= 3
    private boolean informeFinalRevisado;   // los 3 aprobaron el informe
    // Etapa 8 · Sustentación (la última)
    private boolean expedienteSustentacionRecibido; // Secretaría recepcionó y comunicó al Coordinador
    private int numJuradoSustentacion;
    private boolean juradoSustentacionDesignado; // numJuradoSustentacion >= 3
}
