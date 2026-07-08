package unmsm.edu.pe.personas.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.enums.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Actualiza datos de la persona y, opcionalmente, de sus perfiles existentes.
 * No crea perfiles nuevos ni toca la cuenta (users) ni los roles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonaUpdateRequest {

    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombres;
    private Sexo sexo;
    private LocalDate fechaNacimiento;
    private EstadoCivil estadoCivil;
    private String nacionalidad;
    private Procedencia procedencia;

    @Email(message = "El email personal no es válido")
    private String emailPersonal;

    private String celular;
    private Boolean discapacidad;
    private String orcid;

    @Valid
    private EstudiantePerfil estudiante;

    @Valid
    private DocentePerfil docente;

    /** Reemplaza (upsert idempotente) los grados académicos de la persona. Opcional. */
    @Valid
    private List<GradoAcademicoRequest> gradosAcademicos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstudiantePerfil {
        private String codMatricula;
        @Email private String emailInstitucional;
        private Integer anioIngreso;
        private CondicionEstudiante condicion;
        private Financiamiento financiamiento;
        private UUID programaId;
        private String observaciones;

        /** Tutor académico (docente) a asignar/cambiar. Opcional (cambio con historial). */
        private UUID tutorId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocentePerfil {
        @Email private String emailInstitucional;
        private CategoriaDocente categoria;
        private CondicionDocente condicion;

        /** Reemplaza (upsert idempotente) las líneas de investigación del docente. Opcional. */
        @Valid
        private List<DocenteLineaRequest> lineasInvestigacion;
    }
}
