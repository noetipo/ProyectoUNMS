package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Revisor designado de un proyecto (Etapa 5). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisorItem {
    private UUID id;
    private UUID docenteId;
    private String docenteNombre;
    private Integer orden;
    private String estado;       // EstadoRevisor
    private String comentario;
}
