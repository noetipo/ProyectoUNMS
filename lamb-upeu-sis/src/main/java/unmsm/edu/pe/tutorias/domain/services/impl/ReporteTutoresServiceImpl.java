package unmsm.edu.pe.tutorias.domain.services.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tutorias.application.dto.EstudianteSinTutorItem;
import unmsm.edu.pe.tutorias.application.dto.FilaExportReporte;
import unmsm.edu.pe.tutorias.application.dto.ReporteResumen;
import unmsm.edu.pe.tutorias.application.dto.ReporteTutorItem;
import unmsm.edu.pe.tutorias.application.dto.TutorEstudianteItem;
import unmsm.edu.pe.tutorias.application.mapper.ReporteTutoresMapper;
import unmsm.edu.pe.tutorias.domain.repositories.ReporteTutoresRepository;
import unmsm.edu.pe.tutorias.domain.services.ReporteTutoresService;
import unmsm.edu.pe.tutorias.infrastructure.export.ReporteTutoresExcelExporter;
import unmsm.edu.pe.tutorias.infrastructure.export.ReporteTutoresPdfExporter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ReporteTutoresServiceImpl implements ReporteTutoresService {

    @Inject ReporteTutoresRepository repo;
    @Inject ReporteTutoresMapper mapper;
    @Inject ReporteTutoresExcelExporter excelExporter;
    @Inject ReporteTutoresPdfExporter pdfExporter;

    @ConfigProperty(name = "app.tutoria.cupo-default", defaultValue = "20")
    int cupoDefault;

    @Override
    public ReporteResumen resumen(UUID facultadId, UUID programaId) {
        long tutores = repo.contarTutores(null, facultadId, programaId);
        long conTutor = repo.contarEstudiantesConTutor(facultadId, programaId);
        long total = repo.contarEstudiantesTotales(facultadId, programaId);
        long sinTutor = Math.max(0, total - conTutor);
        long promedio = tutores > 0 ? Math.round((double) conTutor / tutores) : 0;
        return ReporteResumen.builder()
                .tutores(tutores)
                .estudiantesConTutor(conTutor)
                .estudiantesSinTutor(sinTutor)
                .promedioPorTutor(promedio)
                .build();
    }

    @Override
    public PageResponse<ReporteTutorItem> listar(String buscar, UUID facultadId, UUID programaId, int page, int size) {
        List<ReporteTutorItem> content = repo.listarTutores(buscar, facultadId, programaId, page, size).stream()
                .map(r -> mapper.toTutorItem(r, cupoDefault))
                .collect(Collectors.toList());
        return PageResponse.of(content, repo.contarTutores(buscar, facultadId, programaId), page, size);
    }

    @Override
    public PageResponse<TutorEstudianteItem> estudiantesDeTutor(UUID docenteId, int page, int size) {
        List<TutorEstudianteItem> content = repo.estudiantesDeTutor(docenteId, page, size).stream()
                .map(mapper::toEstudianteItem)
                .collect(Collectors.toList());
        return PageResponse.of(content, repo.contarEstudiantesDeTutor(docenteId), page, size);
    }

    @Override
    public PageResponse<EstudianteSinTutorItem> estudiantesSinTutor(UUID facultadId, UUID programaId, String buscar, int page, int size) {
        List<EstudianteSinTutorItem> content = repo.estudiantesSinTutor(facultadId, programaId, buscar, page, size).stream()
                .map(mapper::toSinTutorItem)
                .collect(Collectors.toList());
        return PageResponse.of(content, repo.contarEstudiantesSinTutor(facultadId, programaId, buscar), page, size);
    }

    @Override
    public byte[] exportarExcel(String buscar, UUID facultadId, UUID programaId) {
        return excelExporter.export(resumen(facultadId, programaId), filasExport(buscar, facultadId, programaId));
    }

    @Override
    public byte[] exportarPdf(String buscar, UUID facultadId, UUID programaId) {
        return pdfExporter.export(resumen(facultadId, programaId), filasExport(buscar, facultadId, programaId));
    }

    private List<FilaExportReporte> filasExport(String buscar, UUID facultadId, UUID programaId) {
        return repo.datosExport(buscar, facultadId, programaId).stream()
                .map(r -> mapper.toFilaExport(r, cupoDefault))
                .collect(Collectors.toList());
    }
}
