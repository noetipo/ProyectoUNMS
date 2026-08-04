package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.NotFoundException;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.application.dto.*;
import unmsm.edu.pe.tesis.domain.services.MiProyectoService;

import java.util.UUID;

/**
 * Editor "Proyecto de tesis en línea" del ESTUDIANTE (Etapa 4). Todo se resuelve
 * desde el usuario autenticado; nunca se exponen proyectos de otro estudiante.
 */
@Path("/api/mi-proyecto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fase 4 · Mi proyecto", description = "Editor del proyecto en línea del estudiante")
public class MiProyectoResource {

    @Inject MiProyectoService service;
    @Inject SecurityUtils securityUtils;

    /** Herramientas de demo (seed/reset): habilitadas en dev, deshabilitadas en producción. */
    @ConfigProperty(name = "app.demo-tools-enabled", defaultValue = "true")
    boolean demoHabilitado;

    @GET
    @Operation(summary = "Editor completo del proyecto del estudiante autenticado")
    public Response editor() {
        guard();
        return ok("Proyecto recuperado", service.editor());
    }

    @PUT
    @Path("/campos/{campo}")
    @Operation(summary = "Autoguardado de un campo del editor")
    public Response guardarCampo(@PathParam("campo") String campo, GuardarCampoRequest req) {
        guard();
        service.guardarCampo(campo, req != null ? req.getValor() : null);
        return ok("Campo guardado", null);
    }

    @PUT
    @Path("/enfoque")
    @Operation(summary = "Cambiar el enfoque del proyecto (CUANTITATIVO | CUALITATIVO)")
    public Response enfoque(@Valid EnfoqueRequest req) {
        guard();
        service.setEnfoque(req.getEnfoque());
        return ok("Enfoque actualizado", null);
    }

    @POST
    @Path("/enfoque/desbloquear")
    @Operation(summary = "Desbloquear el enfoque para poder cambiarlo")
    public Response desbloquearEnfoque() {
        guard();
        service.desbloquearEnfoque();
        return ok("Enfoque desbloqueado", null);
    }

    @PUT
    @Path("/financiamiento")
    @Operation(summary = "Cambiar el tipo de financiamiento")
    public Response financiamiento(FinanciamientoRequest req) {
        guard();
        service.setFinanciamiento(req != null ? req.getFinanciamiento() : null);
        return ok("Financiamiento actualizado", null);
    }

    // ── Objetivos ──
    @POST @Path("/objetivos")
    public Response agregarObjetivo(ObjetivoRequest req) {
        guard();
        return ok("Objetivo agregado", service.agregarObjetivo(req));
    }

    @PUT @Path("/objetivos/{id}")
    public Response actualizarObjetivo(@PathParam("id") UUID id, ObjetivoRequest req) {
        guard();
        service.actualizarObjetivo(id, req);
        return ok("Objetivo actualizado", null);
    }

    @DELETE @Path("/objetivos/{id}")
    public Response eliminarObjetivo(@PathParam("id") UUID id) {
        guard();
        service.eliminarObjetivo(id);
        return ok("Objetivo eliminado", null);
    }

    // ── Actividades ──
    @POST @Path("/actividades")
    public Response agregarActividad(ActividadRequest req) {
        guard();
        return ok("Actividad agregada", service.agregarActividad(req));
    }

    @PUT @Path("/actividades/{id}")
    public Response actualizarActividad(@PathParam("id") UUID id, ActividadRequest req) {
        guard();
        service.actualizarActividad(id, req);
        return ok("Actividad actualizada", null);
    }

    @DELETE @Path("/actividades/{id}")
    public Response eliminarActividad(@PathParam("id") UUID id) {
        guard();
        service.eliminarActividad(id);
        return ok("Actividad eliminada", null);
    }

    // ── Presupuesto ──
    @POST @Path("/partidas")
    public Response agregarPartida(PartidaRequest req) {
        guard();
        return ok("Partida agregada", service.agregarPartida(req));
    }

    @PUT @Path("/partidas/{id}")
    public Response actualizarPartida(@PathParam("id") UUID id, PartidaRequest req) {
        guard();
        service.actualizarPartida(id, req);
        return ok("Partida actualizada", null);
    }

    @DELETE @Path("/partidas/{id}")
    public Response eliminarPartida(@PathParam("id") UUID id) {
        guard();
        service.eliminarPartida(id);
        return ok("Partida eliminada", null);
    }

    // ── Referencias bibliográficas ──
    @GET @Path("/referencias/por-doi")
    @Operation(summary = "Autocompletar una referencia desde su DOI (CrossRef)")
    public Response referenciaPorDoi(@QueryParam("doi") String doi) {
        guard();
        return ok("Referencia encontrada", service.buscarReferenciaPorDoi(doi));
    }

    @GET @Path("/referencias/buscar")
    @Operation(summary = "Buscar candidatos de referencia por título/autor (sin DOI, CrossRef + OpenAlex)")
    public Response buscarReferencias(@QueryParam("q") String q) {
        guard();
        return ok("Resultados", service.buscarReferenciasPorTitulo(q));
    }

    @POST @Path("/referencias/bibtex")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Rellenar una referencia pegando su BibTeX (tesis, etc.)")
    public Response referenciaBibtex(String bibtex) {
        guard();
        return ok("Referencia leída del BibTeX", service.parsearBibtex(bibtex));
    }

    @POST @Path("/referencias")
    @Operation(summary = "Agregar una referencia bibliográfica estructurada")
    public Response agregarReferencia(ReferenciaRequest req) {
        guard();
        return ok("Referencia agregada", service.agregarReferencia(req));
    }

    @PUT @Path("/referencias/{id}")
    public Response actualizarReferencia(@PathParam("id") UUID id, ReferenciaRequest req) {
        guard();
        service.actualizarReferencia(id, req);
        return ok("Referencia actualizada", null);
    }

    @DELETE @Path("/referencias/{id}")
    public Response eliminarReferencia(@PathParam("id") UUID id) {
        guard();
        service.eliminarReferencia(id);
        return ok("Referencia eliminada", null);
    }

    @PUT @Path("/estilo-cita/{estilo}")
    @Operation(summary = "Fijar el estilo de cita (APA | VANCOUVER | IEEE | HARVARD | MLA | CHICAGO)")
    public Response estiloCita(@PathParam("estilo") String estilo) {
        guard();
        service.setEstiloCita(estilo);
        return ok("Estilo de cita fijado", null);
    }

    @POST @Path("/estilo-cita/desbloquear")
    @Operation(summary = "Desbloquear el estilo de cita para poder cambiarlo")
    public Response desbloquearEstiloCita() {
        guard();
        service.desbloquearEstiloCita();
        return ok("Estilo de cita desbloqueado", null);
    }

    @GET @Path("/documento/{formato}")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Generar el documento del proyecto (formato = pdf | docx)")
    public Response documentoProyecto(@PathParam("formato") String formato) {
        guard();
        var a = service.exportarDocumento(formato);
        return Response.ok(a.contenido(), a.contentType())
                .header("Content-Disposition", "attachment; filename=\"" + a.nombreOriginal() + "\"")
                .build();
    }

    // ── Hitos ──
    @POST @Path("/listo-revision")
    @Operation(summary = "Marcar el proyecto como listo para revisión (avance 100%)")
    public Response marcarListo() {
        guard();
        service.marcarListoRevision();
        return ok("Proyecto enviado a revisión del asesor", null);
    }

    @POST @Path("/reenviar-revision")
    @Operation(summary = "Reenviar el proyecto a revisión tras corregir observaciones")
    public Response reenviarRevision() {
        guard();
        service.reenviarRevision();
        return ok("Proyecto reenviado a revisión del asesor", null);
    }

    @POST @Path("/plan/publicar")
    @Operation(summary = "Publicar el plan de actividades")
    public Response publicarPlan() {
        guard();
        service.publicarPlan();
        return ok("Plan de actividades publicado", null);
    }

    @POST @Path("/observaciones/{campo}/corregir")
    @Operation(summary = "El estudiante confirma la corrección de un ítem observado")
    public Response corregir(@PathParam("campo") String campo, CorregirItemRequest req) {
        guard();
        service.corregirItem(campo, req != null ? req.getRespuesta() : null);
        return ok("Ítem marcado como corregido", null);
    }

    @POST @Path("/revisores/{revisorId}/responder")
    @Operation(summary = "El estudiante levanta las observaciones de un revisor")
    public Response responderRevisor(@PathParam("revisorId") UUID revisorId, CorregirItemRequest req) {
        guard();
        service.responderRevisor(revisorId, req != null ? req.getRespuesta() : null);
        return ok("Respuesta enviada al revisor", null);
    }

    @POST @Path("/jurado-informante/solicitar")
    @Operation(summary = "El estudiante solicita el Jurado Informante del informe final (Etapa 7)")
    public Response solicitarJuradoInformante() {
        guard();
        service.solicitarJuradoInformante();
        return ok("Jurado Informante solicitado", null);
    }

    @POST @Path("/jurado-informante/{revisorId}/responder")
    @Operation(summary = "El estudiante levanta las observaciones de un miembro del Jurado Informante")
    public Response responderJuradoInforme(@PathParam("revisorId") UUID revisorId, CorregirItemRequest req) {
        guard();
        service.responderJuradoInforme(revisorId, req != null ? req.getRespuesta() : null);
        return ok("Respuesta enviada al jurado", null);
    }

    @POST @Path("/turnitin")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el informe de similitud de Turnitin (PDF) + porcentaje")
    public Response turnitin(@RestForm("archivo") FileUpload archivo,
                             @RestForm("porcentaje") Integer porcentaje) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Adjunta el informe de Turnitin")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.subirTurnitin(contenido, archivo.fileName(), archivo.contentType(), porcentaje);
            return ok("Informe de Turnitin subido", null);
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    @GET @Path("/rubrica")
    @Operation(summary = "La rúbrica (en blanco) con la que evaluarán mi proyecto")
    public Response rubrica() {
        guard();
        return Response.ok(ApiResponse.success("Rúbrica de evaluación", service.rubrica())).build();
    }

    @GET @Path("/rubrica/documento")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Word oficial vigente de esa rúbrica")
    public Response rubricaDocumento() {
        guard();
        var a = service.rubricaDocumento();
        return Response.ok(a.contenido(), a.contentType())
                .header("Content-Disposition", "inline; filename=\"" + a.nombreOriginal() + "\"")
                .build();
    }

    @GET @Path("/documentos/{tipo}")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Ver/descargar un documento propio subido (tipo = turnitin | proyecto-final)")
    public Response documento(@PathParam("tipo") String tipo) {
        guard();
        var a = service.descargarDocumento(tipo);
        String ct = a.contentType() != null ? a.contentType() : "application/pdf";
        String name = a.nombreOriginal() != null ? a.nombreOriginal() : tipo + ".pdf";
        return Response.ok(a.contenido(), ct)
                .header("Content-Disposition", "inline; filename=\"" + name + "\"")
                .build();
    }

    @POST @Path("/proyecto-final")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el proyecto en versión final (PDF)")
    public Response proyectoFinal(@RestForm("archivo") FileUpload archivo) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Adjunta el proyecto en versión final")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.subirProyectoFinal(contenido, archivo.fileName(), archivo.contentType());
            return ok("Proyecto versión final subido", null);
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    @POST @Path("/informe-final")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el informe final de la tesis (Etapa 6)")
    public Response informeFinal(@RestForm("archivo") FileUpload archivo) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Adjunta el informe final")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.subirInformeFinal(contenido, archivo.fileName(), archivo.contentType());
            return ok("Informe final subido", null);
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    @POST @Path("/expediente/solicitar-aprobacion")
    @Operation(summary = "Subir el expediente y generar la solicitud de aprobación")
    public Response solicitarAprobacion() {
        guard();
        service.solicitarAprobacion();
        return ok("Solicitud de aprobación registrada", null);
    }

    // ── DEMO / pruebas rápidas ──
    @POST @Path("/demo/seed")
    @Operation(summary = "[DEMO] Rellenar el proyecto con datos de prueba y enviarlo a revisión")
    public Response seedDemo() {
        guard();
        guardDemo();
        service.seedDemo();
        return ok("Datos de prueba cargados y enviados a revisión", null);
    }

    @POST @Path("/demo/reset")
    @Operation(summary = "[DEMO] Reiniciar el flujo (borra observaciones y deja el proyecto como no enviado)")
    public Response resetDemo() {
        guard();
        guardDemo();
        service.resetDemo();
        return ok("Flujo reiniciado", null);
    }

    /** Bloquea las herramientas de demo cuando están deshabilitadas (producción). */
    private void guardDemo() {
        if (!demoHabilitado) {
            throw new NotFoundException("Recurso no disponible");
        }
    }

    private Response ok(String msg, Object data) {
        return Response.ok(data != null ? ApiResponse.success(msg, data) : ApiResponse.success(msg)).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("ESTUDIANTE");
    }
}
