package unmsm.edu.pe.tesis.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Secuencia por año del correlativo de dictámenes (upsert atómico evita choques de concurrencia). */
@Entity
@Table(name = "contador_dictamen")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContadorDictamen {

    @Id
    @Column(name = "anio")
    private Integer anio;

    @Column(name = "correlativo", nullable = false)
    private Integer correlativo;
}
