package unmsm.edu.pe.tesis.domain.enums;

/** Estilo de cita de las referencias bibliográficas del proyecto. */
public enum EstiloCita {
    APA,        // APA 7.ª ed. (autor-año)
    VANCOUVER,  // Vancouver (numérico)
    IEEE,       // IEEE (numérico, ingeniería)
    HARVARD,    // Harvard (autor-año)
    MLA,        // MLA 9.ª ed. (autor-página, humanidades)
    CHICAGO;    // Chicago (autor-año)

    /** Estilos numéricos (cita en el texto = [n]); el resto son autor-año. */
    public boolean esNumerico() {
        return this == VANCOUVER || this == IEEE;
    }
}
