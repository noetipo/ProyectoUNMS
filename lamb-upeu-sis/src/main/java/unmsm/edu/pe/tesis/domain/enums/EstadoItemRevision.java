package unmsm.edu.pe.tesis.domain.enums;

/**
 * Estado de revisión de un campo/ítem del proyecto (máquina de estados del prototipo):
 * {@code SIN_REVISION → OBSERVADO → EN_CORRECCION → CORREGIDO → CONFORME}.
 * <p>El asesor observa (→ OBSERVADO) y da conformidad (→ CONFORME); el estudiante
 * edita (→ EN_CORRECCION) y confirma la corrección (→ CORREGIDO).</p>
 */
public enum EstadoItemRevision {
    SIN_REVISION,
    OBSERVADO,
    EN_CORRECCION,
    CORREGIDO,
    CONFORME
}
