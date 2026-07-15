package unmsm.edu.pe.tesis.domain.enums;

/**
 * Estado del proyecto de tesis dentro de la Etapa 4 (Elaboración del proyecto).
 * <ul>
 *   <li>{@code EN_ELABORACION}: el estudiante está redactando (borrador editable).</li>
 *   <li>{@code EN_REVISION}: el estudiante lo marcó "listo para revisión"; el asesor observa por ítem.</li>
 *   <li>{@code OBSERVADO}: el asesor registró al menos una observación abierta.</li>
 *   <li>{@code CONFORME}: todos los ítems tienen conformidad del asesor.</li>
 *   <li>{@code APROBADO}: el asesor emitió su carta de opinión favorable.</li>
 * </ul>
 */
public enum EstadoProyecto {
    EN_ELABORACION,
    EN_REVISION,
    OBSERVADO,
    CONFORME,
    APROBADO
}
