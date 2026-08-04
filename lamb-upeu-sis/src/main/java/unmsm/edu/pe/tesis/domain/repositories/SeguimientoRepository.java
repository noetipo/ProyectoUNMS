package unmsm.edu.pe.tesis.domain.repositories;

import java.util.List;
import java.util.UUID;

/** Consulta única del tablero de seguimiento (una fila por doctorando con todos los hitos). */
public interface SeguimientoRepository {

    /**
     * Fila por estudiante. Columnas:
     * [0]=persona_id, [1]=apellido_paterno, [2]=apellido_materno, [3]=nombres,
     * [4]=codigo_sistema, [5]=programa_nombre, [6]=tesis_id, [7]=titulo, [8]=linea_nombre,
     * [9]=tesis_estado, [10]=tutor_nombre, [11]=asesor_nombre, [12]=dictamen_firmado(0/1),
     * [13]=listo_revision, [14]=carta_asesor, [15]=expediente_subido, [16]=expediente_recibido,
     * [17]=revisores_conformes, [18]=defensa_programada, [19]=informe_final_aprobado,
     * [20]=jurado_informante_solicitado, [21]=informe_final_revisado,
     * [22]=revisores_designados(count), [23]=rubrica_subida(0/1), [24]=jurado_informe(count),
     * [25]=fecha_defensa.
     *
     * <p>Fechas de los hitos, para calcular hace cuánto no se mueve el expediente:
     * [26]=tesis.fecha_registro, [27]=tutoria.fecha_inicio, [28]=dictamen.fecha_emision,
     * [29]=fecha_listo_revision, [30]=fecha_carta_asesor, [31]=fecha_solicitud_aprobacion,
     * [32]=fecha_recepcion, [33]=fecha_revisores_conformes, [34]=fecha_informe_final,
     * [35]=fecha_jurado_informante, [36]=fecha_informe_revisado.
     *
     * @param programaId filtro opcional por programa
     * @param buscar     texto libre (nombres, apellidos, código, título)
     * @param limite     tope de filas
     */
    List<Object[]> seguimiento(UUID programaId, String buscar, int limite);
}
