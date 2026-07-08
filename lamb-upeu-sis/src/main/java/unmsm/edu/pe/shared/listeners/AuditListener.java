// src/main/java/upeu/edu/pe/shared/listeners/AuditListener.java
package unmsm.edu.pe.shared.listeners;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import unmsm.edu.pe.shared.entities.AuditableEntity;

import java.time.LocalDateTime;

public class AuditListener {

    @PrePersist
    public void prePersist(AuditableEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        String currentUser = getCurrentUser();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
    }

    @PreUpdate
    public void preUpdate(AuditableEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(getCurrentUser());
    }

    private String getCurrentUser() {
        // TODO: Implementar obtención del usuario actual
        // Por ejemplo, desde el contexto de seguridad, JWT, etc.

        // Implementación temporal
        return "system";

        // Ejemplo con contexto de seguridad (cuando implementes autenticación):
        /*
        try {
            // Si usas JWT o algún contexto de seguridad
            return SecurityContext.getCurrentUser();
        } catch (Exception e) {
            return "system";
        }
        */
    }
}