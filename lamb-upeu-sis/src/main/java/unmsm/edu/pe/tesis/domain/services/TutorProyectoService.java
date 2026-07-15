package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.ProyectoBandejaItem;
import unmsm.edu.pe.tesis.application.dto.ProyectoEditorResponse;

import java.util.UUID;

/** Supervisión del TUTOR sobre los proyectos de sus tutorandos (Etapa 4, solo lectura). */
public interface TutorProyectoService {

    PageResponse<ProyectoBandejaItem> bandeja(String buscar, int page, int size);

    ProyectoEditorResponse detalle(UUID tesisId);
}
