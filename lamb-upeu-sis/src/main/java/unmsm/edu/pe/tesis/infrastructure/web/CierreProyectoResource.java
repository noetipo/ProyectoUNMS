package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenAprobacionRequest;
import unmsm.edu.pe.tesis.application.dto.RegistrarResultadoDefensaRequest;
import unmsm.edu.pe.tesis.domain.services.CierreProyectoService;

import java.util.UUID;

/** Secretaría · Etapa 5 (cierre) — resultado de la defensa, dictamen de aprobación y archivo. */
@Path("/api/secretaria/cierre-proyecto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fase 5 · Secretaría (Cierre)", description = "Rúbricas de la defensa, dictamen de aprobación y archivo del expediente")
public class CierreProyectoResource {

    @Inject CierreProyectoService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Proyectos con la defensa programada y lo que falta en cada uno")
    public Response bandeja(@QueryParam("buscar") String buscar) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja de cierre", service.bandeja(buscar))).build();
    }

    @GET
    @Path("/{tesisId}")
    @Operation(summary = "Estado del cierre de un proyecto (defensa, rúbricas, dictamen y archivo)")
    public Response estado(@PathParam("tesisId") UUID tesisId) {
        guard();
        return Response.ok(ApiResponse.success("Cierre del proyecto", service.estado(tesisId))).build();
    }

    // ── Paso 1 · rúbricas y resultado ──

    @POST
    @Path("/{tesisId}/rubrica/{docenteId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Recepcionar la rúbrica firmada de un revisor, con su nota")
    public Response recepcionarRubrica(@PathParam("tesisId") UUID tesisId,
                                       @PathParam("docenteId") UUID docenteId,
                                       @RestForm("puntaje") Integer puntaje,
                                       @RestForm("archivo") FileUpload archivo) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Adjunta la rúbrica firmada")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.recepcionarRubrica(tesisId, docenteId, puntaje, contenido, archivo.fileName(), archivo.contentType());
            return Response.ok(ApiResponse.success("Rúbrica recepcionada")).build();
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    @GET
    @Path("/{tesisId}/rubrica/{docenteId}/raw")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "La rúbrica recepcionada, inline, para verla en el navegador")
    public Response rubricaRaw(@PathParam("tesisId") UUID tesisId, @PathParam("docenteId") UUID docenteId) {
        guard();
        var a = service.documentoRubrica(tesisId, docenteId);
        String ct = a.contentType() != null ? a.contentType() : MediaType.APPLICATION_OCTET_STREAM;
        String name = a.nombreOriginal() != null ? a.nombreOriginal() : "rubrica-defensa";
        return Response.ok(a.contenido(), ct)
                .header("Content-Disposition", "inline; filename=\"" + name + "\"")
                .build();
    }

    @POST
    @Path("/{tesisId}/resultado")
    @Operation(summary = "Registrar el resultado del acto de defensa")
    public Response registrarResultado(@PathParam("tesisId") UUID tesisId, RegistrarResultadoDefensaRequest req) {
        guard();
        service.registrarResultado(tesisId, req);
        return Response.ok(ApiResponse.success("Resultado de la defensa registrado")).build();
    }

    // ── Paso 2 · dictamen de aprobación ──

    @POST
    @Path("/{tesisId}/dictamen")
    @Operation(summary = "Elaborar el dictamen de aprobación del proyecto")
    public Response elaborar(@PathParam("tesisId") UUID tesisId, ElaborarDictamenAprobacionRequest req) {
        guard();
        service.elaborarDictamen(tesisId, req);
        return Response.ok(ApiResponse.success("Dictamen elaborado")).build();
    }

    @GET
    @Path("/{tesisId}/dictamen/documento")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar el dictamen elaborado en pdf o docx")
    public Response documento(@PathParam("tesisId") UUID tesisId,
                              @QueryParam("formato") @DefaultValue("pdf") String formato) {
        guard();
        boolean docx = "docx".equalsIgnoreCase(formato);
        byte[] bytes = service.documentoDictamen(tesisId, formato);
        String ct = docx ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/pdf";
        String name = "dictamen-aprobacion." + (docx ? "docx" : "pdf");
        return Response.ok(bytes, ct)
                .header("Content-Disposition", "inline; filename=\"" + name + "\"")
                .build();
    }

    @POST
    @Path("/{tesisId}/dictamen/firmado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el dictamen de aprobación firmado por el Director")
    public Response subirFirmado(@PathParam("tesisId") UUID tesisId, @RestForm("archivo") FileUpload archivo) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Adjunta el dictamen firmado")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.subirDictamenFirmado(tesisId, contenido, archivo.fileName(), archivo.contentType());
            return Response.ok(ApiResponse.success("Dictamen firmado registrado")).build();
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    // ── Paso 3 · archivo del expediente ──

    @POST
    @Path("/{tesisId}/proyecto-final")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Archivar el proyecto final aprobado (cierra la Etapa 5)")
    public Response archivar(@PathParam("tesisId") UUID tesisId, @RestForm("archivo") FileUpload archivo) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Adjunta el proyecto final")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.archivarProyectoFinal(tesisId, contenido, archivo.fileName(), archivo.contentType());
            return Response.ok(ApiResponse.success("Proyecto archivado; el doctorando pasa a la ejecución de la tesis")).build();
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    @POST
    @Path("/{tesisId}/proyecto-final/del-estudiante")
    @Operation(summary = "Archivar el proyecto final que subió el doctorando (cierra la Etapa 5)")
    public Response archivarDelEstudiante(@PathParam("tesisId") UUID tesisId) {
        guard();
        service.archivarProyectoDelEstudiante(tesisId);
        return Response.ok(ApiResponse.success("Proyecto archivado; el doctorando pasa a la ejecución de la tesis")).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN");
    }
}
