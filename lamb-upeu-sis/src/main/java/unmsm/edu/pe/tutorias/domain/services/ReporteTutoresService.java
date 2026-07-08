package unmsm.edu.pe.tutorias.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tutorias.application.dto.EstudianteSinTutorItem;
import unmsm.edu.pe.tutorias.application.dto.ReporteResumen;
import unmsm.edu.pe.tutorias.application.dto.ReporteTutorItem;
import unmsm.edu.pe.tutorias.application.dto.TutorEstudianteItem;

import java.util.UUID;

public interface ReporteTutoresService {
    ReporteResumen resumen(UUID facultadId, UUID programaId);
    PageResponse<ReporteTutorItem> listar(String buscar, UUID facultadId, UUID programaId, int page, int size);
    PageResponse<TutorEstudianteItem> estudiantesDeTutor(UUID docenteId, int page, int size);
    PageResponse<EstudianteSinTutorItem> estudiantesSinTutor(UUID facultadId, UUID programaId, String buscar, int page, int size);
    byte[] exportarExcel(String buscar, UUID facultadId, UUID programaId);
    byte[] exportarPdf(String buscar, UUID facultadId, UUID programaId);
}
