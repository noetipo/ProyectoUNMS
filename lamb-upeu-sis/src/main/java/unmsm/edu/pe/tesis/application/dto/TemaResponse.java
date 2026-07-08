package unmsm.edu.pe.tesis.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** Tema de investigación registrado (respuesta del coordinador y del perfil). */
@Data
@Builder
public class TemaResponse {
    private UUID tesisId;
    private UUID estudianteId;
    private String titulo;
    private String resumen;
    private UUID lineaInvestigacionId;
    private String lineaNombre;
    private String nivel;
    private String estado;          // tesis.estado
    private String estadoDerivado;  // SIN_ASESOR tras registrar, etc.
    private LocalDate fechaRegistro;
}
