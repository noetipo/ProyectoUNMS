package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
import unmsm.edu.pe.tesis.application.dto.ArchivoDescargable;
import unmsm.edu.pe.tesis.application.dto.ElaborarDictamenRequest;
import unmsm.edu.pe.tesis.application.dto.ObservarDictamenRequest;
import unmsm.edu.pe.tesis.domain.services.SecretariaDictamenService;

import java.util.UUID;

@Path("/api/secretaria/dictamenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Secretaría · Dictámenes de designación", description = "Bandeja, elaboración y firma del dictamen")
public class SecretariaDictamenResource {

    @Inject SecretariaDictamenService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Bandeja de dictámenes (solo tesis con ambos firmados subidos)")
    public Response bandeja(@QueryParam("estado") String estado,
                            @QueryParam("facultadId") String facultadId,
                            @QueryParam("programaId") String programaId,
                            @QueryParam("buscar") String buscar,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja recuperada",
                service.bandeja(estado, uuid(facultadId), uuid(programaId), buscar, page, size))).build();
    }

    @GET
    @Path("/resumen")
    public Response resumen() {
        guard();
        return Response.ok(ApiResponse.success("Resumen", service.resumen())).build();
    }

    @GET
    @Path("/{tesisId}")
    @Operation(summary = "Detalle para elaborar el dictamen")
    public Response detalle(@PathParam("tesisId") UUID tesisId) {
        guard();
        return Response.ok(ApiResponse.success("Detalle", service.detalle(tesisId))).build();
    }

    @GET
    @Path("/{tesisId}/documentos/{tipo}")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar un documento firmado del estudiante (tipo = solicitud | carta)")
    public Response firmadoEstudiante(@PathParam("tesisId") UUID tesisId, @PathParam("tipo") String tipo) {
        guard();
        ArchivoDescargable a = service.descargarFirmadoEstudiante(tesisId, tipo);
        return archivo(a, "inline");
    }

    @POST
    @Path("/{tesisId}")
    @Operation(summary = "Elaborar/generar el dictamen (asigna correlativo)")
    public Response elaborar(@PathParam("tesisId") UUID tesisId, @Valid ElaborarDictamenRequest req) {
        guard();
        return Response.ok(ApiResponse.success("Dictamen elaborado", service.elaborar(tesisId, req))).build();
    }

    @GET
    @Path("/{tesisId}/documento")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar el dictamen elaborado (formato pdf | docx)")
    public Response documento(@PathParam("tesisId") UUID tesisId,
                              @QueryParam("formato") @DefaultValue("pdf") String formato) {
        guard();
        boolean docx = "docx".equalsIgnoreCase(formato != null ? formato.trim() : "pdf");
        byte[] bytes = service.documentoDictamen(tesisId, formato);
        String ct = docx ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/pdf";
        String filename = "dictamen_designacion." + (docx ? "docx" : "pdf");
        return Response.ok(bytes, ct)
                .header("Content-Disposition", (docx ? "attachment" : "inline") + "; filename=\"" + filename + "\"")
                .build();
    }

    @POST
    @Path("/{tesisId}/firmado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir el dictamen firmado por el director")
    public Response subirFirmado(@PathParam("tesisId") UUID tesisId, @RestForm("archivo") FileUpload archivo) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ApiResponse.error("Adjunta el dictamen firmado")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.subirDictamenFirmado(tesisId, contenido, archivo.fileName(), archivo.contentType());
            return Response.ok(ApiResponse.success("Dictamen firmado subido")).build();
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    @POST
    @Path("/{tesisId}/observar")
    @Operation(summary = "Observar los documentos: el estudiante debe re-subir")
    public Response observar(@PathParam("tesisId") UUID tesisId, @Valid ObservarDictamenRequest req) {
        guard();
        service.observar(tesisId, req.getMotivo());
        return Response.ok(ApiResponse.success("Documentos observados")).build();
    }

    private Response archivo(ArchivoDescargable a, String disposition) {
        String ct = a.contentType() != null ? a.contentType() : "application/octet-stream";
        String name = a.nombreOriginal() != null ? a.nombreOriginal() : "documento";
        return Response.ok(a.contenido(), ct)
                .header("Content-Disposition", disposition + "; filename=\"" + name + "\"")
                .build();
    }

    private UUID uuid(String v) {
        return (v != null && !v.isBlank()) ? UUID.fromString(v) : null;
    }

    private void guard() {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN");
    }
}
