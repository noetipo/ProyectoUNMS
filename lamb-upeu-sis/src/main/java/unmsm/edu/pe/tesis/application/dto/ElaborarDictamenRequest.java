package unmsm.edu.pe.tesis.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElaborarDictamenRequest {
    @NotBlank(message = "El N° de expediente es obligatorio")
    private String expediente;

    @NotNull(message = "La fecha de solicitud es obligatoria")
    private LocalDate fechaSolicitud;
}
