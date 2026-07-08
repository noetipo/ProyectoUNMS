package unmsm.edu.pe.tesis.domain.enums;

/**
 * Estado del proceso de una tesis (valor persistido en {@code tesis.estado}).
 * El estado inicial al registrar el tema es {@link #TEMA_REGISTRADO}; los demás
 * valores dejan espacio para las fases posteriores del proceso de tesis.
 *
 * <p>No confundir con el <b>estado derivado</b> del estudiante (Sin tema → Sin
 * asesor → {@code tesis.estado}), que se calcula en
 * {@link unmsm.edu.pe.shared.utils.EstadoDerivado}.</p>
 */
public enum EstadoTesis {
    TEMA_REGISTRADO,
    PROYECTO_PRESENTADO,
    PROYECTO_APROBADO,
    EN_DESARROLLO,
    SUSTENTADO
}
