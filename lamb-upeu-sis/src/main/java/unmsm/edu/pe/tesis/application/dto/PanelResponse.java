package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Panel de inicio. Dos mitades con reglas distintas:
 *
 * <ul>
 *   <li><b>personal</b>: lo que le toca a quien mira, según su rol. Puede nombrar a personas
 *       porque son <i>sus</i> asesorados / <i>su</i> asesor.</li>
 *   <li><b>agregados</b>: cifras del programa. <b>Solo conteos</b>, sin un solo nombre — por eso
 *       cualquier usuario autenticado puede verlos sin exponer a nadie.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PanelResponse {

    /** Rol con el que se armó el panel: ESTUDIANTE | ASESOR | REVISOR | SECRETARIA | COORDINADOR | TUTOR | DOCENTE */
    private String rol;
    private String rolLabel;
    private String saludo;          // "Buenos días, Rosa"
    private String subtitulo;       // frase de contexto según el rol

    /** Tarjetas grandes de arriba: lo importante primero. */
    private List<Metrica> metricas;
    /** Cosas por hacer, ya priorizadas. */
    private List<Pendiente> pendientes;
    /** Solo para el doctorando: su posición en el proceso. */
    private Integer miEtapa;
    private String miEtapaTitulo;
    private Integer miAvancePct;

    /**
     * Gráficos del panel, ya decididos por rol en el servidor: el doctorando recibe los suyos
     * (su avance, sus notas) y la gestión los institucionales. Así el permiso no depende de que
     * la pantalla "oculte" algo, sino de que el dato ni siquiera viaje.
     */
    private List<Grafico> graficos;

    /** Cifras del programa. Solo para roles de gestión/docencia; null para el doctorando. */
    private Agregados agregados;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Metrica {
        private String etiqueta;
        private String valor;
        private String detalle;
        private String icono;     // lucide
        private String tono;      // granate | ambar | esmeralda | cielo | slate
        private String link;      // null si el rol no puede navegar ahí
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Pendiente {
        private String prioridad; // ALTA | MEDIA | INFO
        private String titulo;
        private String detalle;
        private String icono;
        private String link;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Agregados {
        private long doctorandosActivos;
        private long enRevision;
        private long detenidos;        // sin movimiento > 30 días
        private long sustentados;      // del año en curso
        private long docentes;
        private long lineas;
        private List<Conteo> porEtapa;
        private List<Conteo> porLinea;
        private List<Conteo> porAnio;  // registros de tesis por año
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Grafico {
        private String id;
        private String titulo;
        private String subtitulo;
        /** barras | barrasH | dona | radial | area */
        private String tipo;
        private List<String> categorias;
        private List<Serie> series;
        private List<String> colores;
        /** Ocupa las dos columnas de la rejilla (para el embudo, que es largo). */
        private boolean ancho;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Serie {
        private String nombre;
        private List<Long> datos;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Conteo {
        private String etiqueta;
        private long valor;
        private Integer orden;
    }
}
