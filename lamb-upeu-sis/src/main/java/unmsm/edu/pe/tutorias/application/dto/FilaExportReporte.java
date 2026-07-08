package unmsm.edu.pe.tutorias.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Fila plana (tutor + estudiante) para las exportaciones PDF/Excel. */
public record FilaExportReporte(
        UUID tutorId,
        String tutor,
        String grado,
        int cupo,
        String estudiante,
        String codigo,
        String matricula,
        String programa,
        LocalDate inicio
) {}
