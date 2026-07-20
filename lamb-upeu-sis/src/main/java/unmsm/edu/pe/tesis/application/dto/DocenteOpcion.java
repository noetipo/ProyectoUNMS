package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Docente seleccionable como revisor (para el selector del coordinador). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocenteOpcion {
    private UUID id;             // persona_id del docente
    private String nombre;
    private String categoria;
}
