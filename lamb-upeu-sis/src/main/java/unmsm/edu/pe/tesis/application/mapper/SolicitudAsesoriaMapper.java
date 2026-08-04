package unmsm.edu.pe.tesis.application.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.tesis.application.dto.DocenteComboItem;
import unmsm.edu.pe.tesis.application.dto.SolicitudBandejaItem;
import unmsm.edu.pe.tesis.application.dto.SolicitudMiaItem;
import unmsm.edu.pe.tesis.application.dto.SolicitudResponse;
import unmsm.edu.pe.tesis.domain.entities.SolicitudAsesoria;

import java.time.LocalDateTime;
import java.util.UUID;

/** Ensambla respuestas de solicitudes de asesoría (bean CDI puro, sin MapStruct). */
@ApplicationScoped
public class SolicitudAsesoriaMapper {

    public SolicitudResponse toResponse(SolicitudAsesoria s) {
        Estudiante e = s.getEstudiante();
        Docente d = s.getDocente();
        LineaInvestigacion li = s.getLineaInvestigacion();
        return SolicitudResponse.builder()
                .id(s.getId())
                .estudianteId(e != null ? e.getPersonaId() : null)
                .docenteId(d != null ? d.getPersonaId() : null)
                .lineaInvestigacionId(li != null ? li.getId() : null)
                .tituloTentativo(s.getTituloTentativo())
                .mensaje(s.getMensaje())
                .tipo(s.getTipo() != null ? s.getTipo().name() : null)
                .estado(s.getEstado() != null ? s.getEstado().name() : null)
                .fechaSolicitud(s.getFechaSolicitud())
                .fechaRespuesta(s.getFechaRespuesta())
                .motivoRespuesta(s.getMotivoRespuesta())
                .build();
    }

    /** Fila de bandeja del docente (ver orden en SolicitudAsesoriaRepository). */
    public SolicitudBandejaItem toBandejaItem(Object[] r) {
        return SolicitudBandejaItem.builder()
                .id(asUUID(r[0]))
                .estudianteId(asUUID(r[1]))
                .estudianteNombres(asStr(r[2]))
                .estudianteApellidos(join(asStr(r[3]), asStr(r[4])))
                .codigoSistema(asStr(r[5]))
                .programaNombre(asStr(r[6]))
                .lineaNombre(asStr(r[7]))
                .tituloTentativo(asStr(r[8]))
                .tipo(asStr(r[9]))
                .estado(asStr(r[10]))
                .fechaSolicitud(asDateTime(r[11]))
                .fechaRespuesta(asDateTime(r[12]))
                .motivoRespuesta(asStr(r[13]))
                .mensaje(asStr(r[14]))
                .nivel(r.length > 15 ? asStr(r[15]) : null)
                .temaTitulo(r.length > 16 ? asStr(r[16]) : null)
                .temaResumen(r.length > 17 ? asStr(r[17]) : null)
                .tutorNombre(r.length > 18 ? asStr(r[18]) : null)
                .build();
    }

    /** Fila de "mis solicitudes" del estudiante. */
    public SolicitudMiaItem toMiaItem(Object[] r) {
        return SolicitudMiaItem.builder()
                .id(asUUID(r[0]))
                .docenteId(asUUID(r[1]))
                .docenteNombres(asStr(r[2]))
                .docenteApellidos(join(asStr(r[3]), asStr(r[4])))
                .codigoSistema(asStr(r[5]))
                .gradoAcademico(asStr(r[6]))
                .lineaNombre(asStr(r[7]))
                .tituloTentativo(asStr(r[8]))
                .tipo(asStr(r[9]))
                .estado(asStr(r[10]))
                .fechaSolicitud(asDateTime(r[11]))
                .fechaRespuesta(asDateTime(r[12]))
                .motivoRespuesta(asStr(r[13]))
                .mensaje(asStr(r[14]))
                .build();
    }

    /** Docente para el combo (ver orden en DocenteLineaInvestigacionRepository). */
    public DocenteComboItem toDocenteCombo(Object[] r) {
        return DocenteComboItem.builder()
                .id(asUUID(r[0]))
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .gradoAcademico(asStr(r[5]))
                .build();
    }

    // ── helpers ──
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

    private LocalDateTime asDateTime(Object o) {
        return o instanceof LocalDateTime dt ? dt : null;
    }
}
