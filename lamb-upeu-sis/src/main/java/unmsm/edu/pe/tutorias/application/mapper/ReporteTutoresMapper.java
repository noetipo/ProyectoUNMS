package unmsm.edu.pe.tutorias.application.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.shared.utils.EstadoDerivado;
import unmsm.edu.pe.tutorias.application.dto.EstudianteSinTutorItem;
import unmsm.edu.pe.tutorias.application.dto.FilaExportReporte;
import unmsm.edu.pe.tutorias.application.dto.ReporteTutorItem;
import unmsm.edu.pe.tutorias.application.dto.TutorEstudianteItem;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.UUID;

/** Ensambla las filas nativas del reporte de tutores. */
@ApplicationScoped
public class ReporteTutoresMapper {

    public ReporteTutorItem toTutorItem(Object[] r, int cupoDefault) {
        int cupo = r[5] != null ? ((Number) r[5]).intValue() : cupoDefault;
        int estudiantes = asInt(r[6]);
        return ReporteTutorItem.builder()
                .id(asUUID(r[0]))
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .gradoAcademico(asStr(r[4]))
                .cupoMaximo(cupo)
                .estudiantes(estudiantes)
                .cupoLleno(estudiantes >= cupo)
                .programas(asInt(r[7]))
                .build();
    }

    public TutorEstudianteItem toEstudianteItem(Object[] r) {
        return TutorEstudianteItem.builder()
                .estudianteId(asUUID(r[0]))
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .codMatricula(asStr(r[5]))
                .programaNombre(asStr(r[6]))
                .fechaInicio(asLocalDate(r[7]))
                .anioIngreso(r[8] != null ? ((Number) r[8]).intValue() : null)
                .build();
    }

    /**
     * Como {@link #toEstudianteItem} pero con el estado derivado del tema
     * (columnas extra 9=tesis_id, 10=titulo, 11=linea, 12=estado, 13=tiene_asesor).
     * Usado por el panel del tutor.
     */
    public TutorEstudianteItem toTutorandoItem(Object[] r) {
        UUID tesisId = asUUID(r[9]);
        String estadoTesis = asStr(r[12]);
        boolean tieneAsesor = asBool(r[13]);
        return TutorEstudianteItem.builder()
                .estudianteId(asUUID(r[0]))
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .codMatricula(asStr(r[5]))
                .programaNombre(asStr(r[6]))
                .fechaInicio(asLocalDate(r[7]))
                .anioIngreso(r[8] != null ? ((Number) r[8]).intValue() : null)
                .estadoDerivado(EstadoDerivado.resolver(tesisId, estadoTesis, tieneAsesor))
                .tesisTitulo(asStr(r[10]))
                .lineaNombre(asStr(r[11]))
                .build();
    }

    private boolean asBool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return o != null && Boolean.parseBoolean(o.toString());
    }

    public EstudianteSinTutorItem toSinTutorItem(Object[] r) {
        return EstudianteSinTutorItem.builder()
                .estudianteId(asUUID(r[0]))
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .codMatricula(asStr(r[5]))
                .programaNombre(asStr(r[6]))
                .anioIngreso(r[7] != null ? ((Number) r[7]).intValue() : null)
                .build();
    }

    /** Fila plana de exportación (ver orden en ReporteTutoresRepository.datosExport). */
    public FilaExportReporte toFilaExport(Object[] r, int cupoDefault) {
        int cupo = r[5] != null ? ((Number) r[5]).intValue() : cupoDefault;
        String tutor = (join(asStr(r[1]), asStr(r[2])) + ", " + nz(asStr(r[3]))).trim();
        String estudiante = (join(asStr(r[6]), asStr(r[7])) + ", " + nz(asStr(r[8]))).trim();
        return new FilaExportReporte(asUUID(r[0]), tutor, asStr(r[4]), cupo,
                estudiante, asStr(r[9]), asStr(r[10]), asStr(r[11]), asLocalDate(r[12]));
    }

    private String nz(String s) {
        return s != null ? s : "";
    }

    // ── helpers ──
    private String join(String a, String b) {
        return ((a != null ? a : "") + " " + (b != null ? b : "")).trim();
    }

    private int asInt(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private String asStr(Object o) {
        return o != null ? o.toString() : null;
    }

    private LocalDate asLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        return null;
    }

    private UUID asUUID(Object o) {
        if (o == null) return null;
        if (o instanceof UUID u) return u;
        if (o instanceof byte[] b && b.length == 16) { // H2 devuelve uuid nativo como byte[16]
            ByteBuffer bb = ByteBuffer.wrap(b);
            return new UUID(bb.getLong(), bb.getLong());
        }
        return UUID.fromString(o.toString());
    }
}
