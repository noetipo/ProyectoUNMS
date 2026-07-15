package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Fila de la bandeja del asesor: proyectos a revisar. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProyectoBandejaItem {
    private UUID tesisId;
    private UUID proyectoId;
    private String estado;
    private String estudianteApellidos;
    private String estudianteNombres;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
}
