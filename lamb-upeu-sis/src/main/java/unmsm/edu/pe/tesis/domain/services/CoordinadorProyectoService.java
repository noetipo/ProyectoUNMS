package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.DefensaBandejaItem;
import unmsm.edu.pe.tesis.application.dto.DefensaInfo;
import unmsm.edu.pe.tesis.application.dto.DesignarRevisoresRequest;
import unmsm.edu.pe.tesis.application.dto.DocenteOpcion;
import unmsm.edu.pe.tesis.application.dto.ProgramarDefensaRequest;
import unmsm.edu.pe.tesis.application.dto.RevisorItem;

import java.util.List;
import java.util.UUID;

/** Casos de uso del COORDINADOR sobre la defensa del proyecto (Etapa 5). */
public interface CoordinadorProyectoService {

    /** Bandeja: proyectos con expediente recepcionado por Secretaría. */
    PageResponse<DefensaBandejaItem> bandeja(String buscar, int page, int size);

    /** Docentes seleccionables como revisores. */
    List<DocenteOpcion> docentesDisponibles(UUID tesisId);

    /** Revisores ya designados de un proyecto. */
    List<RevisorItem> revisores(UUID tesisId);

    /** Designa los revisores del proyecto (2 docentes). */
    void designarRevisores(UUID tesisId, DesignarRevisoresRequest req);

    /** Información de la defensa programada (jurado + fecha). */
    DefensaInfo defensa(UUID tesisId);

    /** Programa la defensa: Jurado Examinador + fecha/hora/lugar (requiere revisores conformes). */
    void programarDefensa(UUID tesisId, ProgramarDefensaRequest req);

    /** Miembros del Jurado Informante ya designados. */
    List<unmsm.edu.pe.tesis.application.dto.InformeRevisorItem> juradoInforme(UUID tesisId);

    /** Designa los 3 miembros del Jurado Informante del informe final (el 1.º es Presidente). */
    void designarJuradoInforme(UUID tesisId, unmsm.edu.pe.tesis.application.dto.DesignarJuradoInformeRequest req);
}
