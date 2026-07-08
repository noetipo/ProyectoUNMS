package unmsm.edu.pe.shared.utils;

import java.util.UUID;

/**
 * Estado <b>derivado</b> de un estudiante respecto a su tesis — ÚNICA fuente de
 * verdad, consumida por el reporte del coordinador, el panel del tutor y el perfil
 * del estudiante. No se guarda como flag: se calcula con esta prioridad:
 *
 * <ol>
 *   <li>{@link #SIN_TEMA}: no hay tesis activa.</li>
 *   <li>{@link #SIN_ASESOR}: hay tesis pero aún no tiene asesor (asesoría tipo ASESOR).</li>
 *   <li>El valor de {@code tesis.estado} (p. ej. TEMA_REGISTRADO): hay tesis y asesor.</li>
 * </ol>
 */
public final class EstadoDerivado {

    public static final String SIN_TEMA = "SIN_TEMA";
    public static final String SIN_ASESOR = "SIN_ASESOR";

    private EstadoDerivado() {
    }

    /**
     * @param tesisActivaId id de la tesis activa del estudiante (null si no tiene)
     * @param estadoTesis   valor de {@code tesis.estado} (null si no hay tesis)
     * @param tieneAsesor   si existe una asesoría tipo ASESOR para esa tesis
     * @return el código de estado derivado (SIN_TEMA | SIN_ASESOR | &lt;estado tesis&gt;)
     */
    public static String resolver(UUID tesisActivaId, String estadoTesis, boolean tieneAsesor) {
        if (tesisActivaId == null) {
            return SIN_TEMA;
        }
        if (!tieneAsesor) {
            return SIN_ASESOR;
        }
        return estadoTesis != null ? estadoTesis : SIN_ASESOR;
    }

    /** Etiqueta legible para UI/exportes. */
    public static String etiqueta(String codigo) {
        if (codigo == null) {
            return "";
        }
        switch (codigo) {
            case SIN_TEMA:
                return "Sin tema";
            case SIN_ASESOR:
                return "Sin asesor";
            case "TEMA_REGISTRADO":
                return "Tema registrado";
            case "PROYECTO_PRESENTADO":
                return "Proyecto presentado";
            case "PROYECTO_APROBADO":
                return "Proyecto aprobado";
            case "EN_DESARROLLO":
                return "En desarrollo";
            case "SUSTENTADO":
                return "Sustentado";
            default:
                return codigo;
        }
    }
}
