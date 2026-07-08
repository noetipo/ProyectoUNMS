package unmsm.edu.pe.tesis.domain.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.configuracion.domain.repositories.LemaAnualRepository;
import unmsm.edu.pe.configuracion.domain.repositories.ParametroSistemaRepository;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.repositories.PersonaCargoRepository;
import unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository;
import unmsm.edu.pe.tesis.application.util.FormatoDocumentos;
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.repositories.TesisRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resuelve los marcadores {...} de las plantillas de asesoría a partir de la solicitud,
 * reutilizando la configuración existente (lema activo, ciudad, director vigente, facultad,
 * grados). El resultado (Map) se guarda como snapshot y se usa para renderizar .docx/.pdf.
 */
@ApplicationScoped
public class ResolverDatosPlantillaService {

    private static final String CARGO_DIRECTOR = "Director de la Unidad de Posgrado";

    @Inject LemaAnualRepository lemaRepository;
    @Inject ParametroSistemaRepository parametroRepository;
    @Inject PersonaCargoRepository personaCargoRepository;
    @Inject PersonaGradoAcademicoRepository gradoRepository;
    @Inject TesisRepository tesisRepository;

    /** Marcadores de la Solicitud de designación (se resuelve al crear la solicitud). */
    public Map<String, String> resolverSolicitud(SolicitudAsesoria sol, LocalDate fecha) {
        Estudiante e = sol.getEstudiante();
        Docente asesor = sol.getDocente();
        Persona pe = e != null ? e.getPersona() : null;
        Persona pa = asesor != null ? asesor.getPersona() : null;
        Tesis tesis = tesisActiva(e, sol);

        Map<String, String> m = comunes(sol, fecha, tesis);
        m.put("GRADO_ASESOR", tratamiento(asesor, pa));
        m.put("NOMBRE_ASESOR", nombresApellidos(pa));
        m.put("APELLIDOS_NOMBRES_ESTUDIANTE", apellidosNombres(pe));
        m.put("CORREO_ESTUDIANTE", correoEstudiante(e, pe));
        m.put("CELULAR_ESTUDIANTE", pe != null ? nz(pe.getCelular()) : "");
        return m;
    }

    /** Marcadores de la Carta de aceptación (se resuelve al aceptar el docente). */
    public Map<String, String> resolverCarta(SolicitudAsesoria sol, LocalDate fecha) {
        Estudiante e = sol.getEstudiante();
        Docente asesor = sol.getDocente();
        Persona pe = e != null ? e.getPersona() : null;
        Persona pa = asesor != null ? asesor.getPersona() : null;
        Tesis tesis = tesisActiva(e, sol);

        Map<String, String> m = comunes(sol, fecha, tesis);
        m.put("CONDICION", asesor != null ? FormatoDocumentos.condicionTexto(
                asesor.getCondicion() != null ? asesor.getCondicion().name() : null) : "");
        m.put("TIPO_ASESOR", FormatoDocumentos.tipoAsesorTexto(sol.getTipo() != null ? sol.getTipo().name() : null));
        m.put("GRADO_ESTUDIANTE", tratamiento(null, pe));
        m.put("NOMBRE_ESTUDIANTE", nombresApellidos(pe));
        m.put("NOMBRE_APELLIDOS_ASESOR", nombresApellidos(pa));
        return m;
    }

    /** Marcadores del Dictamen de designación (se resuelve al elaborar). */
    public Map<String, String> resolverDictamen(unmsm.edu.pe.tesis.domain.entities.DictamenDesignacion dic,
                                                 Tesis tesis, Estudiante e,
                                                 Docente asesor, Docente coasesor, LocalDate fecha) {
        Persona pe = e != null ? e.getPersona() : null;
        Persona pa = asesor != null ? asesor.getPersona() : null;
        ProgramaPosgrado prog = e != null ? e.getPrograma() : null;
        String nivelEnum = tesis != null && tesis.getNivel() != null ? tesis.getNivel().name()
                : (prog != null && prog.getNivel() != null ? prog.getNivel().name() : null);

        Map<String, String> m = new LinkedHashMap<>();
        m.put("FACULTAD", facultad(prog));
        m.put("NIVEL_SECCION", FormatoDocumentos.nivelSeccion(nivelEnum));
        m.put("CIUDAD", parametro("CIUDAD_EMISION", ""));
        m.put("FECHA_LARGA", FormatoDocumentos.fechaLarga(fecha));
        m.put("ANIO", FormatoDocumentos.anio(fecha));
        m.put("NUMERO_DICTAMEN", nz(dic.getNumero()));
        m.put("EXPEDIENTE", nz(dic.getExpediente()));
        m.put("FECHA_SOLICITUD", dic.getFechaSolicitud() != null ? FormatoDocumentos.fechaLarga(dic.getFechaSolicitud()) + " del " + dic.getFechaSolicitud().getYear() : "");
        m.put("CONSIDERANDOS", parametro("CONSIDERANDOS_DICTAMEN", ""));
        m.put("GRADO_ASESOR", tratamiento(pa));
        m.put("NOMBRE_ASESOR", nombresApellidos(pa));
        // Bloque de co-asesor solo si existe.
        String bloque = "";
        if (coasesor != null && coasesor.getPersona() != null) {
            bloque = ", y como Co-asesor al " + tratamiento(coasesor.getPersona()) + " " + nombresApellidos(coasesor.getPersona());
        }
        m.put("BLOQUE_COASESOR", bloque);
        m.put("TITULO_TESIS", tesis != null ? nz(tesis.getTitulo()) : "");
        m.put("TRATAMIENTO_ESTUDIANTE", pe != null ? FormatoDocumentos.tratamientoPorSexo(pe.getSexo() != null ? pe.getSexo().name() : null) : "");
        m.put("NOMBRE_ESTUDIANTE", nombresApellidos(pe));
        m.put("PROGRAMA", prog != null ? FormatoDocumentos.programaConNivel(nivelEnum, prog.getNombre()) : "");
        m.put("NOMBRE_DIRECTOR", nombreDirector());
        m.put("PIE", parametro("PIE_DICTAMEN", ""));
        return m;
    }

    // ── comunes a ambos documentos ─────────────────────────────────────────
    private Map<String, String> comunes(SolicitudAsesoria sol, LocalDate fecha, Tesis tesis) {
        Estudiante e = sol.getEstudiante();
        ProgramaPosgrado prog = e != null ? e.getPrograma() : null;
        String nivelEnum = tesis != null && tesis.getNivel() != null ? tesis.getNivel().name()
                : (prog != null && prog.getNivel() != null ? prog.getNivel().name() : null);

        Map<String, String> m = new LinkedHashMap<>();
        m.put("LEMA_ANIO", lemaDelAnio(fecha));
        m.put("CIUDAD", parametro("CIUDAD_EMISION", ""));
        m.put("FECHA_LARGA", FormatoDocumentos.fechaLarga(fecha));
        m.put("ANIO", FormatoDocumentos.anio(fecha));
        m.put("NOMBRE_DIRECTOR", nombreDirector());
        m.put("FACULTAD", facultad(prog));
        m.put("NIVEL_ADJ", FormatoDocumentos.nivelAdjetivo(nivelEnum));
        m.put("NIVEL", FormatoDocumentos.nivelTexto(nivelEnum));
        m.put("PROGRAMA", prog != null ? FormatoDocumentos.programaSinNivel(prog.getNombre()) : "");
        m.put("TITULO_TESIS", tesis != null ? nz(tesis.getTitulo()) : nz(sol.getTituloTentativo()));
        return m;
    }

    private Tesis tesisActiva(Estudiante e, SolicitudAsesoria sol) {
        if (e == null) return null;
        return tesisRepository.tesisActivaDeEstudiante(e.getPersonaId()).orElse(null);
    }

    private String lemaDelAnio(LocalDate fecha) {
        int anio = (fecha != null ? fecha : LocalDate.now()).getYear();
        return lemaRepository.buscarActivoPorAnio(anio).map(l -> nz(l.getTexto())).orElse("");
    }

    private String parametro(String clave, String def) {
        return parametroRepository.buscarPorClave(clave).map(p -> nz(p.getValor())).orElse(def);
    }

    private String nombreDirector() {
        return personaCargoRepository.buscarCargoActualPorNombre(CARGO_DIRECTOR)
                .map(pc -> nombresApellidos(pc.getPersona()))
                .filter(s -> !s.isBlank())
                .orElseGet(() -> parametro("NOMBRE_DIRECTOR", ""));
    }

    private String facultad(ProgramaPosgrado prog) {
        String nombre = (prog != null && prog.getFacultad() != null) ? prog.getFacultad().getNombre() : null;
        return FormatoDocumentos.facultadSinPrefijo(nombre);
    }

    private String tratamiento(Docente docente, Persona p) {
        return tratamiento(p);
    }

    private String tratamiento(Persona p) {
        if (p == null) return "";
        String grado = gradoRepository.gradoPrincipal(p.getId());
        return FormatoDocumentos.tratamientoPorGrado(grado, p.getSexo() != null ? p.getSexo().name() : null);
    }

    private String correoEstudiante(Estudiante e, Persona p) {
        if (e != null && e.getEmailInstitucional() != null && !e.getEmailInstitucional().isBlank()) {
            return e.getEmailInstitucional();
        }
        return p != null ? nz(p.getEmailPersonal()) : "";
    }

    /** "Nombres ApellidoPaterno ApellidoMaterno". */
    private String nombresApellidos(Persona p) {
        if (p == null) return "";
        return (nz(p.getNombres()) + " " + nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno()))
                .trim().replaceAll("\\s+", " ");
    }

    /** "ApellidoPaterno ApellidoMaterno, Nombres". */
    private String apellidosNombres(Persona p) {
        if (p == null) return "";
        String ap = (nz(p.getApellidoPaterno()) + " " + nz(p.getApellidoMaterno())).trim().replaceAll("\\s+", " ");
        return (ap + ", " + nz(p.getNombres())).trim();
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
