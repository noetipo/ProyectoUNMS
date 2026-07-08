package unmsm.edu.pe.catalog.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.shared.entities.AuditableEntity;
import unmsm.edu.pe.shared.listeners.AuditListener;
import unmsm.edu.pe.shared.annotations.Normalize;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditListener.class)
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @Normalize(Normalize.NormalizeType.UPPERCASE)
    private String name;

    @Column(length = 255)
    @Normalize(Normalize.NormalizeType.UPPERCASE)
    private String description;
}