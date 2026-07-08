package unmsm.edu.pe.personas.application.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.application.dto.AgregarPerfilDocenteRequest;
import unmsm.edu.pe.personas.application.dto.CrearPersonaRequest;
import unmsm.edu.pe.personas.application.dto.DocenteListItem;
import unmsm.edu.pe.personas.application.dto.EstudianteListItem;
import unmsm.edu.pe.personas.application.dto.PersonaListItem;
import unmsm.edu.pe.personas.application.dto.PersonaResponse;
import unmsm.edu.pe.personas.application.dto.ProgramaPosgradoResponse;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import unmsm.edu.pe.personas.domain.entities.PersonaGradoAcademico;

/**
 * Ensambla entidades del contexto personas a partir de los DTOs de request y
 * construye las respuestas. Bean CDI puro (sin MapStruct) para poder probarlo
 * directamente en tests unitarios.
 */
@ApplicationScoped
public class PersonaMapper {

    public Persona toPersona(CrearPersonaRequest.PersonaDatos d) {
        return Persona.builder()
                .tipoDocumento(d.getTipoDocumento())
                .numeroDocumento(trim(d.getNumeroDocumento()))
                .apellidoPaterno(d.getApellidoPaterno())
                .apellidoMaterno(d.getApellidoMaterno())
                .nombres(d.getNombres())
                .sexo(d.getSexo())
                .fechaNacimiento(d.getFechaNacimiento())
                .estadoCivil(d.getEstadoCivil())
                .nacionalidad(d.getNacionalidad())
                .procedencia(d.getProcedencia())
                .emailPersonal(d.getEmailPersonal())
                .celular(d.getCelular())
                .discapacidad(d.getDiscapacidad())
                .orcid(trimToNull(d.getOrcid()))
                .build();
    }

    public Estudiante toEstudiante(CrearPersonaRequest.EstudianteDatos d, Persona persona, ProgramaPosgrado programa) {
        return Estudiante.builder()
                .persona(persona)
                .codigoSistema(trimToNull(d.getCodigoSistema()))
                .codMatricula(trimToNull(d.getCodMatricula()))
                .emailInstitucional(trimToNull(d.getEmailInstitucional()))
                .anioIngreso(d.getAnioIngreso())
                .condicion(d.getCondicion())
                .financiamiento(d.getFinanciamiento())
                .observaciones(d.getObservaciones())
                .programa(programa)
                .build();
    }

    public Docente toDocente(CrearPersonaRequest.DocenteDatos d, Persona persona) {
        return Docente.builder()
                .persona(persona)
                .codigoSistema(trimToNull(d.getCodigoSistema()))
                .emailInstitucional(trimToNull(d.getEmailInstitucional()))
                .categoria(d.getCategoria())
                .condicion(d.getCondicion())
                .build();
    }

    public Docente toDocente(AgregarPerfilDocenteRequest d, Persona persona) {
        return Docente.builder()
                .persona(persona)
                .codigoSistema(trimToNull(d.getCodigoSistema()))
                .emailInstitucional(trimToNull(d.getEmailInstitucional()))
                .categoria(d.getCategoria())
                .condicion(d.getCondicion())
                .build();
    }

    /** Convierte los grados de la persona al DTO de respuesta. */
    public List<PersonaResponse.GradoAcademicoItem> toGradoItems(List<PersonaGradoAcademico> grados) {
        if (grados == null) {
            return List.of();
        }
        return grados.stream()
                .map(g -> PersonaResponse.GradoAcademicoItem.builder()
                        .id(g.getId())
                        .grado(g.getGrado() != null ? g.getGrado().name() : null)
                        .anio(g.getAnio())
                        .universidad(g.getUniversidad())
                        .principal(g.getPrincipal())
                        .build())
                .collect(Collectors.toList());
    }

    public PersonaResponse toResponse(Persona persona, Estudiante estudiante, Docente docente, List<String> roles) {
        List<String> perfiles = new ArrayList<>();
        if (estudiante != null) {
            perfiles.add("ESTUDIANTE");
        }
        if (docente != null) {
            perfiles.add("DOCENTE");
        }

        String username = persona.getUser() != null ? persona.getUser().getUsername() : null;

        return PersonaResponse.builder()
                .id(persona.getId())
                .nombres(persona.getNombres())
                .apellidos(apellidos(persona))
                .tipoDocumento(persona.getTipoDocumento() != null ? persona.getTipoDocumento().name() : null)
                .apellidoPaterno(persona.getApellidoPaterno())
                .apellidoMaterno(persona.getApellidoMaterno())
                .sexo(persona.getSexo() != null ? persona.getSexo().name() : null)
                .fechaNacimiento(persona.getFechaNacimiento() != null ? persona.getFechaNacimiento().toString() : null)
                .estadoCivil(persona.getEstadoCivil() != null ? persona.getEstadoCivil().name() : null)
                .nacionalidad(persona.getNacionalidad())
                .procedencia(persona.getProcedencia() != null ? persona.getProcedencia().name() : null)
                .discapacidad(persona.getDiscapacidad())
                .numeroDocumento(persona.getNumeroDocumento())
                .emailPersonal(persona.getEmailPersonal())
                .celular(persona.getCelular())
                .orcid(persona.getOrcid())
                .username(username)
                .perfiles(perfiles)
                .roles(roles != null ? roles : List.of())
                .estudiante(estudiante == null ? null : toEstudianteResumen(estudiante))
                .docente(docente == null ? null : toDocenteResumen(docente))
                .build();
    }

    public ProgramaPosgradoResponse toProgramaResponse(ProgramaPosgrado p) {
        return ProgramaPosgradoResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .nivel(p.getNivel() != null ? p.getNivel().name() : null)
                .facultadId(p.getFacultad() != null ? p.getFacultad().getId() : null)
                .facultadNombre(p.getFacultad() != null ? p.getFacultad().getNombre() : null)
                .build();
    }

    private PersonaResponse.EstudianteResumen toEstudianteResumen(Estudiante e) {
        ProgramaPosgrado prog = e.getPrograma();
        return PersonaResponse.EstudianteResumen.builder()
                .codigoSistema(e.getCodigoSistema())
                .codMatricula(e.getCodMatricula())
                .emailInstitucional(e.getEmailInstitucional())
                .anioIngreso(e.getAnioIngreso())
                .programaId(prog != null ? prog.getId() : null)
                .programaNombre(prog != null ? prog.getNombre() : null)
                .facultadId(prog != null && prog.getFacultad() != null ? prog.getFacultad().getId() : null)
                .facultadNombre(prog != null && prog.getFacultad() != null ? prog.getFacultad().getNombre() : null)
                .condicion(e.getCondicion() != null ? e.getCondicion().name() : null)
                .financiamiento(e.getFinanciamiento() != null ? e.getFinanciamiento().name() : null)
                .observaciones(e.getObservaciones())
                .build();
    }

    private PersonaResponse.DocenteResumen toDocenteResumen(Docente d) {
        // gradoAcademico (principal) lo completa el service desde la lista de grados de la persona.
        return PersonaResponse.DocenteResumen.builder()
                .codigoSistema(d.getCodigoSistema())
                .emailInstitucional(d.getEmailInstitucional())
                .categoria(d.getCategoria() != null ? d.getCategoria().name() : null)
                .condicion(d.getCondicion() != null ? d.getCondicion().name() : null)
                .build();
    }

    // ── Mapeo de filas de listados nativos ──────────────────────────────────

    public PersonaListItem toPersonaListItem(Object[] r) {
        List<String> perfiles = new ArrayList<>();
        if (asBool(r[8])) perfiles.add("ESTUDIANTE");
        if (asBool(r[9])) perfiles.add("DOCENTE");
        return PersonaListItem.builder()
                .id(asUUID(r[0]))
                .numeroDocumento(asStr(r[1]))
                .apellidos(join(asStr(r[2]), asStr(r[3])))
                .nombres(asStr(r[4]))
                .emailPersonal(asStr(r[5]))
                .celular(asStr(r[6]))
                .activo(asBool(r[7]))
                .perfiles(perfiles)
                .build();
    }

    public EstudianteListItem toEstudianteListItem(Object[] r) {
        return EstudianteListItem.builder()
                .personaId(asUUID(r[0]))
                .numeroDocumento(asStr(r[1]))
                .apellidos(join(asStr(r[2]), asStr(r[3])))
                .nombres(asStr(r[4]))
                .codigoSistema(asStr(r[5]))
                .codMatricula(asStr(r[6]))
                .emailInstitucional(asStr(r[7]))
                .anioIngreso(asInt(r[8]))
                .condicion(asStr(r[9]))
                .financiamiento(asStr(r[10]))
                .programaId(asUUID(r[11]))
                .programaNombre(asStr(r[12]))
                .nivel(asStr(r[13]))
                .activo(asBool(r[14]))
                .build();
    }

    public DocenteListItem toDocenteListItem(Object[] r) {
        long ases = asLong(r[11]);
        long jur = asLong(r[12]);
        return DocenteListItem.builder()
                .personaId(asUUID(r[0]))
                .numeroDocumento(asStr(r[1]))
                .apellidos(join(asStr(r[2]), asStr(r[3])))
                .nombres(asStr(r[4]))
                .codigoSistema(asStr(r[5]))
                .emailInstitucional(asStr(r[6]))
                .gradoAcademico(asStr(r[7]))
                .categoria(asStr(r[8]))
                .condicion(asStr(r[9]))
                .activo(asBool(r[10]))
                .asesorias(ases)
                .jurados(jur)
                .carga(ases + jur)
                .build();
    }

    private String join(String a, String b) {
        return ((a != null ? a : "") + " " + (b != null ? b : "")).trim();
    }

    private UUID asUUID(Object o) {
        if (o == null) return null;
        if (o instanceof UUID u) return u;
        return UUID.fromString(o.toString());
    }

    private String asStr(Object o) {
        return o != null ? o.toString() : null;
    }

    private Integer asInt(Object o) {
        return o instanceof Number n ? n.intValue() : null;
    }

    private long asLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private boolean asBool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return o != null && Boolean.parseBoolean(o.toString());
    }

    private String apellidos(Persona p) {
        String paterno = p.getApellidoPaterno() != null ? p.getApellidoPaterno() : "";
        String materno = p.getApellidoMaterno() != null ? p.getApellidoMaterno() : "";
        return (paterno + " " + materno).trim();
    }

    private String trim(String s) {
        return s != null ? s.trim() : null;
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
