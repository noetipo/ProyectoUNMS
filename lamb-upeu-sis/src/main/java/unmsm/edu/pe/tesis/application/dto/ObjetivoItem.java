package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Objetivo específico del proyecto. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjetivoItem {
    private UUID id;
    private Integer orden;
    private String texto;
}
