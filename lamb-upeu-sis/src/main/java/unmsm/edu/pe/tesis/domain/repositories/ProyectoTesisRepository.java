package unmsm.edu.pe.tesis.domain.repositories;

import unmsm.edu.pe.tesis.domain.entities.ProyectoTesis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoTesisRepository {
    ProyectoTesis save(ProyectoTesis proyecto);
    Optional<ProyectoTesis> buscarPorId(UUID id);
    Optional<ProyectoTesis> buscarPorTesisId(UUID tesisId);

    /** Bandeja del asesor: proyectos marcados listos para revisión (o ya observados/conformes)
     *  del asesor dado. Filas [tesisId, proyectoId, estado, apPat, apMat, nombres, codigo, programa, titulo]. */
    List<Object[]> bandejaDeAsesor(UUID asesorId, String estado, String buscar, int page, int size);
    long contarBandejaDeAsesor(UUID asesorId, String estado, String buscar);

    /** Bandeja del tutor: proyectos de sus tutorandos (tutoría vigente). Mismas columnas que la del asesor. */
    List<Object[]> bandejaDeTutor(UUID tutorId, String buscar, int page, int size);
    long contarBandejaDeTutor(UUID tutorId, String buscar);

    /** Bandeja de la Secretaría (Etapa 5): expedientes con solicitud de aprobación enviada.
     *  Filas [tesisId, apPat, apMat, nombres, codigo, programa, titulo, fechaSolicitud, recibido]. */
    List<Object[]> bandejaExpedientes(String buscar, int page, int size);
    long contarBandejaExpedientes(String buscar);

    /** Bandeja del coordinador: proyectos con expediente recepcionado (Etapa 5). */
    List<Object[]> bandejaDefensa(String buscar, int page, int size);
    long contarBandejaDefensa(String buscar);
}
