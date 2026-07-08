package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Docente disponible para el combo del formulario (filtrado por línea). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocenteComboItem {
    private UUID id;
    private String nombres;
    private String apellidos;
    private String codigoSistema;
    private String gradoAcademico;
}
