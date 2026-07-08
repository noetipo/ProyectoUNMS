package unmsm.edu.pe.tesis.application.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import unmsm.edu.pe.shared.utils.EstadoDerivado;
import unmsm.edu.pe.tesis.application.dto.EstudianteTemaItem;

import java.nio.ByteBuffer;
import java.util.UUID;

/** Ensambla las filas nativas del reporte del coordinador. */
@ApplicationScoped
public class CoordinadorTemaMapper {

    /** Ver orden de columnas en {@code CoordinadorTemaRepository.estudiantesTema}. */
    public EstudianteTemaItem toItem(Object[] r) {
        UUID tesisId = asUUID(r[8]);
        String estado = asStr(r[11]);
        boolean tieneAsesor = asBool(r[12]);
        return EstudianteTemaItem.builder()
                .estudianteId(asUUID(r[0]))
                .apellidos(join(asStr(r[1]), asStr(r[2])))
                .nombres(asStr(r[3]))
                .codigoSistema(asStr(r[4]))
                .codMatricula(asStr(r[5]))
                .programaNombre(asStr(r[6]))
                .nivel(asStr(r[7]))
                .conTema(tesisId != null)
                .tesisId(tesisId)
                .titulo(asStr(r[9]))
                .lineaNombre(asStr(r[10]))
                .estado(estado)
                .estadoDerivado(EstadoDerivado.resolver(tesisId, estado, tieneAsesor))
                .build();
    }

    // ── helpers ──
    private String join(String a, String b) {
        return ((a != null ? a : "") + " " + (b != null ? b : "")).trim();
    }

    private String asStr(Object o) {
        return o != null ? o.toString() : null;
    }

    private boolean asBool(Object o) {
        if (o instanceof Boolean b) {
            return b;
        }
        if (o instanceof Number n) {
            return n.intValue() != 0;
        }
        return o != null && Boolean.parseBoolean(o.toString());
    }

    private UUID asUUID(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof UUID u) {
            return u;
        }
        if (o instanceof byte[] b && b.length == 16) { // H2 devuelve uuid nativo como byte[16]
            ByteBuffer bb = ByteBuffer.wrap(b);
            return new UUID(bb.getLong(), bb.getLong());
        }
        return UUID.fromString(o.toString());
    }
}
