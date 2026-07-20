package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Miembro del Jurado Examinador. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JuradoItem {
    private UUID docenteId;
    private String docenteNombre;
    private String rol;      // PRESIDENTE | MIEMBRO | ASESOR
    private Integer orden;
}
