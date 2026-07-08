package unmsm.edu.pe.personas.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaResponse {
    private UUID id;
    private String nombres;
    private String apellidos;
    // Campos individuales para edición (además de 'apellidos' combinado para mostrar)
    private String tipoDocumento;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String sexo;
    private String fechaNacimiento;   // ISO yyyy-MM-dd
    private String estadoCivil;
    private String nacionalidad;
    private String procedencia;
    private Boolean discapacidad;
    private String numeroDocumento;
    private String emailPersonal;
    private String celular;
    private String orcid;
    private String username;
    private List<String> perfiles;
    private List<String> roles;
    private EstudianteResumen estudiante;
    private DocenteResumen docente;
    /** Grados académicos de la persona (lista; para editar/mostrar). */
    private List<GradoAcademicoItem> gradosAcademicos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstudianteResumen {
        private String codigoSistema;
        private String codMatricula;
        private String emailInstitucional;
        private Integer anioIngreso;
        private UUID programaId;
        private String programaNombre;
        private UUID facultadId;
        private String facultadNombre;
        private String condicion;
        private String financiamiento;
        private String observaciones;
        private UUID tutorId;
        private String tutorNombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocenteResumen {
        private String codigoSistema;
        private String emailInstitucional;
        /** Grado principal del docente (derivado de la lista de grados de la persona). */
        private String gradoAcademico;
        private String categoria;
        private String condicion;
        private List<LineaInvestigacionItem> lineasInvestigacion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradoAcademicoItem {
        private UUID id;
        private String grado;
        private Integer anio;
        private String universidad;
        private Boolean principal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineaInvestigacionItem {
        private UUID id;
        private String nombre;
        private Boolean esPrincipal;
    }
}
