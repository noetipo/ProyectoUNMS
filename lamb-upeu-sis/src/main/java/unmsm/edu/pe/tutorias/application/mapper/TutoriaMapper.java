package unmsm.edu.pe.tutorias.application.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.tutorias.application.dto.EstudianteAsignableItem;
import unmsm.edu.pe.tutorias.application.dto.TutorComboItem;
import unmsm.edu.pe.tutorias.application.dto.TutoriaHistorialItem;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;

import java.util.UUID;

/** Ensambla respuestas de tutorías (bean CDI puro, sin MapStruct). */
@ApplicationScoped
public class TutoriaMapper {

    @Inject unmsm.edu.pe.personas.domain.repositories.PersonaGradoAcademicoRepository personaGradoRepository;

    /** Fila de listarTutores. `cupoDefault` se usa cuando el docente no tiene cupo configurado. */
    public TutorComboItem toTutorCombo(Object[] r, int cupoDefault) {
        int cupo = r[6] != null ? ((Number) r[6]).intValue() : cupoDefault;
        int actuales = r[7] != null ? ((Number) r[7]).intValue() : 0;
        return TutorComboItem.builder()
                .id(asUUID(r[0]))
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .gradoAcademico(asStr(r[5]))
                .cupoMaximo(cupo)
                .estudiantesActuales(actuales)
                .disponible(actuales < cupo)
                .esTutor(r.length > 8 && asBool(r[8]))
                .build();
    }

    /** Fila de listarEstudiantesAsignables. */
    public EstudianteAsignableItem toEstudianteAsignable(Object[] r) {
        UUID tutorId = asUUID(r[7]);
        return EstudianteAsignableItem.builder()
                .estudianteId(asUUID(r[0]))
                .nombres(asStr(r[1]))
                .apellidos(join(asStr(r[2]), asStr(r[3])))
                .codigoSistema(asStr(r[4]))
                .programaId(asUUID(r[5]))
                .programaNombre(asStr(r[6]))
                .tutorId(tutorId)
                .tutorNombre(tutorId == null ? null : apellidosNombre(asStr(r[8]), asStr(r[9]), asStr(r[10])))
                .build();
    }

    /** Fila del historial (accede a docente.persona; llamar dentro de una transacción). */
    public TutoriaHistorialItem toHistorialItem(Tutoria t) {
        Docente d = t.getDocente();
        Persona dp = d != null ? d.getPersona() : null;
        String nombre = dp == null ? null
                : (join(dp.getApellidoPaterno(), dp.getApellidoMaterno()) + ", " + dp.getNombres()).trim();
        return TutoriaHistorialItem.builder()
                .id(t.getId())
                .docenteId(d != null ? d.getPersonaId() : null)
                .docenteNombre(nombre)
                .gradoAcademico(d != null ? personaGradoRepository.gradoPrincipal(d.getPersonaId()) : null)
                .fechaInicio(t.getFechaInicio())
                .fechaFin(t.getFechaFin())
                .actual(t.getActual())
                .motivoCambio(t.getMotivoCambio())
                .build();
    }

    // ── helpers ──
    /** "Apellido Paterno Apellido Materno, Nombres". */
    private String apellidosNombre(String nombres, String apPat, String apMat) {
        return (join(apPat, apMat) + ", " + (nombres != null ? nombres : "")).trim();
    }

    private String join(String a, String b) {
        return ((a != null ? a : "") + " " + (b != null ? b : "")).trim();
    }

    private UUID asUUID(Object o) {
        if (o == null) return null;
        if (o instanceof UUID u) return u;
        // H2 devuelve las columnas uuid de consultas nativas como byte[16].
        if (o instanceof byte[] b && b.length == 16) {
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(b);
            return new UUID(bb.getLong(), bb.getLong());
        }
        return UUID.fromString(o.toString());
    }

    private String asStr(Object o) {
        return o != null ? o.toString() : null;
    }

    private boolean asBool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return o != null && Boolean.parseBoolean(o.toString());
    }
}
