package unmsm.edu.pe.shared.entities;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import unmsm.edu.pe.shared.context.AuditContext;
import unmsm.edu.pe.shared.utils.NormalizeProcessor;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class AuditableEntity {

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        String currentUser = getCurrentUser();
        this.createdBy = currentUser;
        this.updatedBy = currentUser;

        // Procesar anotaciones @Normalize
        NormalizeProcessor.processNormalizeAnnotations(this);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = getCurrentUser();

        // Procesar anotaciones @Normalize
        NormalizeProcessor.processNormalizeAnnotations(this);
    }

    private String getCurrentUser() {
        try {
            AuditContext auditContext = CDI.current().select(AuditContext.class).get();
            String currentUser = auditContext.getCurrentUser();
            return currentUser != null ? currentUser : "system";
        } catch (Exception e) {
            System.out.println("Warning: Could not get current user from audit context: " + e.getMessage());
            return "system";
        }
    }
}