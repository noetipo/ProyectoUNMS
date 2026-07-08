package unmsm.edu.pe.security.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "modules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Module extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // ✅ Cambio aquí
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 100, nullable = false)
    private String subtitle;

    @Column(length = 100, nullable = false)
    private String type;

    @Column(length = 20)
    private String code;

    @Column(length = 100)
    private String icon;

    @Column(nullable = false)
    private Boolean status = true;

    @Column(nullable = false)
    private Integer moduleOrder;

    @Column(length = 500, nullable = false)
    private String link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_module_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ParentModule parentModule;

    private LocalDateTime deletedAt;

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }
}
