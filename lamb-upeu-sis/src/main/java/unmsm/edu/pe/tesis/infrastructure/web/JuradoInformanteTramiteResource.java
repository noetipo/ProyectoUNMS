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
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenExpeditoRequest;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenJuradoInformeRequest;
import unmsm.edu.pe.tesis.domain.services.JuradoInformanteTramiteService;

import java.util.UUID;

/** Secretaría · Etapa 7 — trámite del Jurado Informante y Dictamen de Expedito. */
@Path("/api/secretaria/jurado-informante")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fase 7 · Secretaría (Jurado Informante)", description = "Recepción, dictamen de designación, archivo y Dictamen de Expedito")
public class JuradoInformanteTramiteResource {

    @Inject JuradoInformanteTramiteService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Expedientes del Jurado Informante y lo que falta en cada uno")
    public Response bandeja(@QueryParam("buscar") String buscar) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja de Jurado Informante", service.bandeja(buscar))).build();
    }

    @GET
    @Path("/{tesisId}")
    @Operation(summary = "Detalle del trámite del Jurado Informante de un expediente")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return Response.ok(ApiResponse.success("Trámite del Jurado Informante", service.detalle(tesisId))).build();
    }

    @POST
    @Path("/{tesisId}/recepcionar")
    @Operation(summary = "Recepcionar el expediente y comunicar al Coordinador")
    public Response recepcionar(@PathParam("tesisId") UUID tesisId) {
        guard();
        service.recepcionar(tesisId);
        return Response.ok(ApiResponse.success("Expediente recepcionado")).build();
    }

    @POST
    @Path("/{tesisId}/dictamen")
    @Operation(summary = "Elaborar el dictamen de designación del Jurado Informante")
    public Response elaborar(@PathParam("tesisId") UUID tesisId, ElaborarDictamenJuradoInformeRequest req) {
        guard();
        service.elaborarDictamen(tesisId, req);
        return Response.ok(ApiResponse.success("Dictamen elaborado")).build();
    }

    @GET
    @Path("/{tesisId}/dictamen/documento")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar el dictamen de designación en pdf o docx")
    public Response documentoDictamen(@PathParam("tesisId") UUID tesisId,
                                      @QueryParam("formato") @DefaultValue("pdf") String formato) {
        guard();
        boolean docx = "docx".equalsIgnoreCase(formato);
        byte[] bytes = service.documentoDictamen(tesisId, formato);
        String ct = docx ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/pdf";
        return Response.ok(bytes, ct)
                .header("Content-Disposition", "inline; filename=\"dictamen-jurado-informante." + (docx ? "docx" : "pdf") + "\"")
                .build();
    }

    @POST
    @Path("/{tesisId}/dictamen/firmado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el dictamen de designación firmado")
    public Response subirDictamenFirmado(@PathParam("tesisId") UUID tesisId, @RestForm("archivo") FileUpload archivo) {
        guard();
        byte[] contenido = leer(archivo, "Adjunta el dictamen firmado");
        service.subirDictamenFirmado(tesisId, contenido, archivo.fileName(), archivo.contentType());
        return Response.ok(ApiResponse.success("Dictamen firmado registrado")).build();
    }

    @POST
    @Path("/{tesisId}/archivar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Archivar el expediente del Jurado Informante (tras su conformidad)")
    public Response archivar(@PathParam("tesisId") UUID tesisId, @RestForm("archivo") FileUpload archivo) {
        guard();
        byte[] contenido = leer(archivo, "Adjunta el expediente a archivar");
        service.archivarExpediente(tesisId, contenido, archivo.fileName(), archivo.contentType());
        return Response.ok(ApiResponse.success("Expediente del Jurado Informante archivado")).build();
    }

    @POST
    @Path("/{tesisId}/expedito")
    @Operation(summary = "Elaborar el Dictamen de Expedito")
    public Response elaborarExpedito(@PathParam("tesisId") UUID tesisId, ElaborarDictamenExpeditoRequest req) {
        guard();
        service.elaborarDictamenExpedito(tesisId, req);
        return Response.ok(ApiResponse.success("Dictamen de Expedito elaborado")).build();
    }

    @GET
    @Path("/{tesisId}/expedito/documento")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar el Dictamen de Expedito en pdf o docx")
    public Response documentoExpedito(@PathParam("tesisId") UUID tesisId,
                                      @QueryParam("formato") @DefaultValue("pdf") String formato) {
        guard();
        boolean docx = "docx".equalsIgnoreCase(formato);
        byte[] bytes = service.documentoExpedito(tesisId, formato);
        String ct = docx ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/pdf";
        return Response.ok(bytes, ct)
                .header("Content-Disposition", "inline; filename=\"dictamen-expedito." + (docx ? "docx" : "pdf") + "\"")
                .build();
    }

    @POST
    @Path("/{tesisId}/expedito/firmado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el Dictamen de Expedito firmado")
    public Response subirExpeditoFirmado(@PathParam("tesisId") UUID tesisId, @RestForm("archivo") FileUpload archivo) {
        guard();
        byte[] contenido = leer(archivo, "Adjunta el Dictamen de Expedito firmado");
        service.subirDictamenExpeditoFirmado(tesisId, contenido, archivo.fileName(), archivo.contentType());
        return Response.ok(ApiResponse.success("Dictamen de Expedito firmado registrado")).build();
    }

    private byte[] leer(FileUpload archivo, String mensajeSiFalta) {
        if (archivo == null) {
            throw new BusinessException(mensajeSiFalta);
        }
        try {
            return java.nio.file.Files.readAllBytes(archivo.uploadedFile());
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    private void guard() {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN");
    }
}
