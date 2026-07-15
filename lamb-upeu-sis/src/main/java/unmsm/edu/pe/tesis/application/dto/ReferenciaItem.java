package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Referencia bibliográfica estructurada + su vista formateada en el estilo del proyecto. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenciaItem {
    private UUID id;
    private Integer orden;
    private String tipo;
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
    /** Cita ya formateada en el estilo actual (entrada de bibliografía). */
    private String formateada;
    /** Cita parentética para el texto: [n] o (Autor, año). */
    private String citaTexto;
    /** Cita narrativa para el texto: Autor (año) o Autor [n]. */
    private String citaNarrativa;
}
