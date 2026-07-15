package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

/** Alta/edición de una referencia bibliográfica. */
@Data
public class ReferenciaRequest {
    private String tipo;        // TipoReferencia
    private String autores;
    private String anio;
    private String titulo;
    private String fuente;
    private String volumen;
    private String numero;
    private String paginas;
    private String editorial;
    private String ciudad;
    private String doi;
    private String url;
    private String fechaAcceso;
    private Integer orden;
}
