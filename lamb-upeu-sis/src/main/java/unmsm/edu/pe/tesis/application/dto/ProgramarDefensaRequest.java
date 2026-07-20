package unmsm.edu.pe.tesis.application.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Programación de la defensa: Jurado Examinador (Presidente + 2 miembros) + fecha/hora/lugar. */
@Data
public class ProgramarDefensaRequest {
    private UUID presidenteId;
    private List<UUID> miembroIds;   // 2 docentes
    private LocalDate fecha;
    private String hora;             // "10:00"
    private String lugar;
}
