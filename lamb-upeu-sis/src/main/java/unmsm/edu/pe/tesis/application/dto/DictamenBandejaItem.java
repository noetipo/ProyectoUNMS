package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictamenBandejaItem {
    private UUID tesisId;
    private String estudianteNombres;
    private String estudianteApellidos;
    private String codigoSistema;
    private String programaNombre;
    private String tituloTesis;
    private String asesorNombre;
    private String coasesorNombre;
    private String estadoDictamen;
    private String numero;
}
