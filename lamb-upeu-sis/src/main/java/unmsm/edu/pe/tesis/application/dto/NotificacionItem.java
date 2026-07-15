package unmsm.edu.pe.tesis.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Notificación accionable para la campanita, derivada del rol del usuario. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionItem {
    private String id;
    private String title;
    private String description;
    private String link;    // ruta del front, p. ej. /admin/secretaria-defensa
    private String icon;    // nombre lucide
    private String fecha;   // formateada (dd/MM/yyyy) o null
}
