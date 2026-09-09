package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Información de la defensa programada (jurado + fecha + dictamen). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefensaInfo {
    private boolean programada;
    private LocalDate fecha;
    private String hora;
    private String lugar;
    private String modalidad;
    private String modalidadLabel;
    private String enlace;
    private String dictamenNumero;
    private List<JuradoItem> jurado;
}
