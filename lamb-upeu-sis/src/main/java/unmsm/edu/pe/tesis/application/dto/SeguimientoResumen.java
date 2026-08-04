package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Tarjetas de estadística del tablero de seguimiento. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeguimientoResumen {

    private long total;
    /** Alumnos con el proceso terminado (sustentados). */
    private long finalizados;
    /** Alumnos cuya acción pendiente le corresponde a la Secretaría. */
    private long pendientesSecretaria;
    /** Alumnos que aún no tienen tema registrado. */
    private long sinTema;
    /** Alumnos sin movimiento hace más de 30 días (excluye a los que ya terminaron). */
    private long detenidos;
    /** Dictámenes de proyecto que vencen en 6 meses o menos (incluye los ya vencidos). */
    private long dictamenesPorVencer;
    /** Conteo por etapa (1..8), en orden. */
    private List<EtapaConteo> porEtapa;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EtapaConteo {
        private int numero;
        private String titulo;
        private long cantidad;
    }
}
