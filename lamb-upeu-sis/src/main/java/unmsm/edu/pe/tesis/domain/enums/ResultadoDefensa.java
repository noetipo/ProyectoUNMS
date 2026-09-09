package unmsm.edu.pe.tesis.domain.enums;

/** Resultado del acto de defensa, según lo que acuerdan los revisores. */
public enum ResultadoDefensa {
    APROBADO,
    APROBADO_CON_OBSERVACIONES,
    DESAPROBADO;

    public String etiqueta() {
        return switch (this) {
            case APROBADO -> "Aprobado";
            case APROBADO_CON_OBSERVACIONES -> "Aprobado con observaciones";
            case DESAPROBADO -> "Desaprobado";
        };
    }

    /** Solo los resultados favorables dan lugar al dictamen de aprobación. */
    public boolean favorable() {
        return this != DESAPROBADO;
    }
}
