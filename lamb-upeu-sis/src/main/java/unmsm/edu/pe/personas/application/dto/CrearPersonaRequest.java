package unmsm.edu.pe.personas.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import unmsm.edu.pe.personas.domain.enums.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearPersonaRequest {

    @NotNull(message = "Los datos de la persona son obligatorios")
    @Valid
    private PersonaDatos persona;

    @NotNull(message = "Los datos de la cuenta son obligatorios")
    @Valid
    private CuentaDatos cuenta;

    @Valid
    private EstudianteDatos estudiante;

    @Valid
    private DocenteDatos docente;

    /** Grados académicos de la persona (lista; uno principal). Réplica del patrón de listas de persona. */
    @Valid
    private List<GradoAcademicoRequest> gradosAcademicos;

    /** Historiales de cargos y centros laborales (opcional en el alta multipart). */
    private PerfilCompletoData perfil;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonaDatos {
        @NotNull(message = "El tipo de documento es obligatorio")
        private TipoDocumento tipoDocumento;

        @NotBlank(message = "El número de documento es obligatorio")
        private String numeroDocumento;

        @NotBlank(message = "El apellido paterno es obligatorio")
        private String apellidoPaterno;

        private String apellidoMaterno;

        @NotBlank(message = "Los nombres son obligatorios")
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
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuentaDatos {
        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        private String email;

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstudianteDatos {
        private String codigoSistema;
        private String codMatricula;

        @Email(message = "El email institucional no es válido")
        private String emailInstitucional;

        private Integer anioIngreso;

        @NotNull(message = "El programa de posgrado es obligatorio para el perfil estudiante")
        private UUID programaId;

        private CondicionEstudiante condicion;
        private Financiamiento financiamiento;
        private String observaciones;

        /** Tutor académico (docente) a asignar al estudiante. Opcional. */
        private UUID tutorId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocenteDatos {
        private String codigoSistema;

        @Email(message = "El email institucional no es válido")
        private String emailInstitucional;

        private CategoriaDocente categoria;
        private CondicionDocente condicion;

        /** Líneas de investigación (especialidad) del docente. Opcional. */
        @Valid
        private List<DocenteLineaRequest> lineasInvestigacion;
    }
}
