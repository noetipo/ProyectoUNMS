package unmsm.edu.pe.tesis.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Fila del reporte del coordinador: un estudiante con/sin tema registrado. */
@Data
@Builder
public class EstudianteTemaItem {
    private UUID estudianteId;
    private String nombres;
    private String apellidos;
    private String codigoSistema;
    private String codMatricula;
    private String programaNombre;
    private String nivel;

    private boolean conTema;
    private UUID tesisId;
    private String titulo;
    private String lineaNombre;
    private String estado;          // tesis.estado (null si sin tema)
    private String estadoDerivado;  // SIN_TEMA | SIN_ASESOR | <estado tesis>
}
