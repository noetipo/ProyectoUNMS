package unmsm.edu.pe.security.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Fila del reporte de usuarios: usuario + datos de su persona + roles. */
@Data
@Builder
public class UserReportItem {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
    private boolean active;

    // Persona vinculada (puede ser null: p. ej. admin sin persona)
    private UUID personaId;
    private String personaNombre;   // "Apellidos, Nombres"
    private String numeroDocumento;

    private List<String> roles;     // códigos de rol asignados (activos)
}
