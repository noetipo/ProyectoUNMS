package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.tesis.application.dto.*;

import java.util.UUID;

/** Casos de uso del ESTUDIANTE sobre su proyecto de tesis (Etapa 4). */
public interface MiProyectoService {

    ProyectoEditorResponse editor();

    /** La rúbrica (en blanco) con la que se evaluará su proyecto, según su enfoque. */
    unmsm.edu.pe.tesis.application.dto.RubricaAlumnoResponse rubrica();

    /** El Word oficial vigente de esa rúbrica. */
    ArchivoDescargable rubricaDocumento();

    void guardarCampo(String campo, String valor);

    void setEnfoque(String enfoque);

    /** Desbloquea el enfoque para poder cambiarlo. */
    void desbloquearEnfoque();

    void setFinanciamiento(String financiamiento);

    ObjetivoItem agregarObjetivo(ObjetivoRequest req);
    void actualizarObjetivo(UUID id, ObjetivoRequest req);
    void eliminarObjetivo(UUID id);

    ActividadItem agregarActividad(ActividadRequest req);
    void actualizarActividad(UUID id, ActividadRequest req);
    void eliminarActividad(UUID id);

    // ── Referencias bibliográficas (estructuradas, auto-formateadas) ──
    /** Consulta un DOI en CrossRef y devuelve los campos para autocompletar (no persiste). */
    ReferenciaRequest buscarReferenciaPorDoi(String doi);
    /** Busca candidatos por título/autor (sin DOI) para autocompletar. */
    java.util.List<ReferenciaRequest> buscarReferenciasPorTitulo(String q);
    /** Parsea una entrada BibTeX (Google Scholar / repositorios) a una referencia. */
    ReferenciaRequest parsearBibtex(String bibtex);
    ReferenciaItem agregarReferencia(ReferenciaRequest req);
    void actualizarReferencia(UUID id, ReferenciaRequest req);
    void eliminarReferencia(UUID id);
    /** Fija el estilo de cita del proyecto (queda bloqueado; un documento usa un solo estilo). */
    void setEstiloCita(String estilo);
    /** Desbloquea el estilo de cita para poder cambiarlo. */
    void desbloquearEstiloCita();
    /** Genera el documento del proyecto (formato = pdf | docx) con las referencias formateadas. */
    ArchivoDescargable exportarDocumento(String formato);

    PartidaItem agregarPartida(PartidaRequest req);
    void actualizarPartida(UUID id, PartidaRequest req);
    void eliminarPartida(UUID id);

    /** Marca el proyecto como listo para revisión (exige avance 100%). */
    void marcarListoRevision();

    /** Reenvía el proyecto a revisión tras corregir observaciones (de OBSERVADO a EN_REVISION). */
    void reenviarRevision();

    /** Publica el plan de actividades (exige al menos una actividad). */
    void publicarPlan();

    /** Sube el informe de similitud de Turnitin (carga manual) + porcentaje. */
    void subirTurnitin(byte[] contenido, String nombreOriginal, String contentType, Integer porcentaje);

    /** Sube el proyecto en versión final (PDF) para el expediente. */
    void subirProyectoFinal(byte[] contenido, String nombreOriginal, String contentType);

    /** Sube el informe final de la tesis (Etapa 6 · ejecución). */
    void subirInformeFinal(byte[] contenido, String nombreOriginal, String contentType);

    /** Descarga un documento propio que el estudiante subió (tipo = turnitin | proyecto-final). */
    ArchivoDescargable descargarDocumento(String tipo);

    /** El estudiante confirma la corrección de un ítem observado. */
    void corregirItem(String campo, String respuesta);

    /** El estudiante responde (levanta) las observaciones de un revisor del proyecto (Etapa 5). */
    void responderRevisor(UUID revisorId, String respuesta);

    /** El estudiante solicita el Jurado Informante del informe final (Etapa 7). */
    void solicitarJuradoInformante();

    /** El estudiante responde (levanta) las observaciones de un miembro del Jurado Informante (Etapa 7). */
    void responderJuradoInforme(UUID revisorId, String respuesta);

    /** Sube el expediente y genera la solicitud de aprobación (fin de la Etapa 4). */
    void solicitarAprobacion();

    /** [DEMO] Rellena el proyecto con datos de prueba y lo envía a revisión (para probar el flujo rápido). */
    void seedDemo();

    /** [DEMO] Reinicia el flujo: borra observaciones/correcciones y deja el proyecto como "aún no enviado". */
    void resetDemo();
}
