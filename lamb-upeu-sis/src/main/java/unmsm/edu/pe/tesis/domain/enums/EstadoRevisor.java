package unmsm.edu.pe.tesis.domain.enums;

/** Estado de un revisor del proyecto (Etapa 5 · defensa del proyecto). */
public enum EstadoRevisor {
    DESIGNADO,   // el coordinador lo designó; aún no evalúa
    OBSERVADO,   // evaluó con la rúbrica y dejó observaciones
    CONFORME     // dio conformidad al proyecto
}
