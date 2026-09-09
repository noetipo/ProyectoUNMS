package unmsm.edu.pe.tesis.domain.enums;

/** Cómo se realiza la defensa oral del proyecto: define si hace falta ambiente físico, enlace o ambos. */
public enum ModalidadDefensa {
    PRESENCIAL,
    VIRTUAL,
    HIBRIDA;

    /** Etiqueta para documentos y pantallas. */
    public String etiqueta() {
        return switch (this) {
            case PRESENCIAL -> "Presencial";
            case VIRTUAL -> "Virtual";
            case HIBRIDA -> "Híbrida";
        };
    }

    /** Las modalidades con asistencia remota necesitan enlace de sesión. */
    public boolean requiereEnlace() {
        return this != PRESENCIAL;
    }

    /** Las modalidades con asistencia física necesitan aula o ambiente. */
    public boolean requiereLugar() {
        return this != VIRTUAL;
    }
}
