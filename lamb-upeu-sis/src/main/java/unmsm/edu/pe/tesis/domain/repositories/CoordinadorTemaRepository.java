package unmsm.edu.pe.tesis.domain.repositories;

import java.util.List;
import java.util.UUID;

/** Consultas del reporte del coordinador (estudiantes con/sin tema registrado). */
public interface CoordinadorTemaRepository {

    /**
     * Fila por estudiante. Columnas:
     * [0]=persona_id, [1]=apellido_paterno, [2]=apellido_materno, [3]=nombres,
     * [4]=codigo_sistema, [5]=cod_matricula, [6]=programa_nombre, [7]=programa_nivel,
     * [8]=tesis_id, [9]=titulo, [10]=linea_nombre, [11]=tesis_estado, [12]=tiene_asesor,
     * [13]=tutor_nombre (tutoría vigente; null si aún no le designan tutor).
     *
     * @param conTema true=solo con tema, false=solo sin tema, null=todos
     */
    List<Object[]> estudiantesTema(UUID facultadId, UUID programaId, Boolean conTema, String buscar, int page, int size);

    long contarEstudiantesTema(UUID facultadId, UUID programaId, Boolean conTema, String buscar);

    long contarTotal(UUID facultadId, UUID programaId);

    long contarConTema(UUID facultadId, UUID programaId);

    /** Estudiantes <b>con tema</b> pero sin tutoría vigente: el paso pendiente tras registrar el tema. */
    long contarSinTutor(UUID facultadId, UUID programaId);
}
