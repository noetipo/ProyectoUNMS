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
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.ProgramarSustentacionRequest;
import unmsm.edu.pe.tesis.application.dto.RegistrarActaSustentacionRequest;
import unmsm.edu.pe.tesis.domain.services.SustentacionService;

import java.util.UUID;

/** Secretaría · Etapa 8 (la última) — trámite de Sustentación de la tesis. */
@Path("/api/secretaria/sustentacion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fase 8 · Secretaría (Sustentación)", description = "Dictamen, programación del acto, acta y cierre del proceso de titulación")
public class SustentacionResource {

    @Inject SustentacionService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Expedientes en trámite de sustentación y lo que falta en cada uno")
    public Response bandeja(@QueryParam("buscar") String buscar) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja de sustentación", service.bandeja(buscar))).build();
    }

    @GET
    @Path("/{tesisId}")
    @Operation(summary = "Detalle del trámite de sustentación de un expediente")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return Response.ok(ApiResponse.success("Trámite de sustentación", service.detalle(tesisId))).build();
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
    @Operation(summary = "Elaborar el dictamen de designación del Jurado de Sustentación")
    public Response elaborar(@PathParam("tesisId") UUID tesisId, ElaborarDictamenSustentacionRequest req) {
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
                .header("Content-Disposition", "inline; filename=\"dictamen-sustentacion." + (docx ? "docx" : "pdf") + "\"")
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
    @Path("/{tesisId}/programar")
    @Operation(summary = "Coordinar modalidad, lugar y fecha de la sustentación")
    public Response programar(@PathParam("tesisId") UUID tesisId, ProgramarSustentacionRequest req) {
        guard();
        service.programar(tesisId, req);
        return Response.ok(ApiResponse.success("Sustentación programada")).build();
    }

    @POST
    @Path("/{tesisId}/acta")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Registrar el resultado del acto y subir el Acta firmada (concluye la tesis)")
    public Response registrarActa(@PathParam("tesisId") UUID tesisId,
                                  @RestForm("resultado") String resultado,
                                  @RestForm("observacion") String observacion,
                                  @RestForm("archivo") FileUpload archivo) {
        guard();
        byte[] contenido = leer(archivo, "Adjunta el Acta de sustentación firmada");
        RegistrarActaSustentacionRequest req = new RegistrarActaSustentacionRequest();
        req.setResultado(resultado);
        req.setObservacion(observacion);
        service.registrarActa(tesisId, req, contenido, archivo.fileName(), archivo.contentType());
        return Response.ok(ApiResponse.success("Acta registrada; el proceso de titulación ha concluido")).build();
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
