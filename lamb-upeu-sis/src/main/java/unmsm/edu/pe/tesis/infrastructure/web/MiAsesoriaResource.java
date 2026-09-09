package unmsm.edu.pe.tesis.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tesis.domain.services.MiAsesoriaService;

/**
 * Bandeja del estudiante para iniciar su asesoría: tema, asesores sugeridos por su
 * tutor, estado de la solicitud y descarga de documentos (PDF) generados al vuelo.
 * Todo se resuelve desde el usuario autenticado; nunca se exponen datos de otro estudiante.
 */
@Path("/api/mi-asesoria")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mi asesoría", description = "Bandeja del estudiante: tema, asesores sugeridos y documentos")
public class MiAsesoriaResource {

    @Inject MiAsesoriaService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Bandeja de asesoría del estudiante autenticado")
    public Response bandeja() {
        guard();
        return Response.ok(ApiResponse.success("Bandeja recuperada", service.bandeja())).build();
    }

    @GET
    @Path("/documentos/{tipo}")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar un documento propio (SOLICITUD_ASESORIA | CARTA_ACEPTACION) en formato pdf o docx")
    public Response documento(@PathParam("tipo") String tipo,
                              @QueryParam("formato") @DefaultValue("pdf") String formato) {
        guard();
        boolean docx = "docx".equalsIgnoreCase(formato != null ? formato.trim() : "pdf");
        byte[] bytes = service.documentoBytes(tipo, formato);
        String ext = docx ? "docx" : "pdf";
        String contentType = docx
                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/pdf";
        String filename = tipo.toLowerCase() + "." + ext;
        return Response.ok(bytes, contentType)
                .header("Content-Disposition", (docx ? "attachment" : "inline") + "; filename=\"" + filename + "\"")
                .build();
    }

    @POST
    @Path("/documentos/{tipo}/firmado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "El estudiante sube su documento firmado (tipo = solicitud | carta). Permite reemplazar")
    public Response subirFirmado(@PathParam("tipo") String tipo,
                                 @org.jboss.resteasy.reactive.RestForm("archivo") org.jboss.resteasy.reactive.multipart.FileUpload archivo) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Debes adjuntar el archivo firmado")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.subirFirmado(tipo, contenido, archivo.fileName(), archivo.contentType());
            return Response.ok(ApiResponse.success("Documento firmado subido")).build();
        } catch (java.io.IOException e) {
            throw new unmsm.edu.pe.shared.exceptions.BusinessException("No se pudo leer el archivo subido");
        }
    }

    @POST
    @Path("/documentos/enviar")
    @Operation(summary = "Confirma el envío de los documentos firmados a Secretaría")
    public Response enviarDocumentos() {
        guard();
        service.enviarDocumentosFirmados();
        return Response.ok(ApiResponse.success("Documentos enviados a Secretaría")).build();
    }

    @GET
    @Path("/dictamen")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar el dictamen firmado emitido por la secretaría")
    public Response dictamen() {
        guard();
        var arch = service.descargarDictamenEmitido();
        return Response.ok(arch.contenido(), arch.contentType() != null ? arch.contentType() : "application/pdf")
                .header("Content-Disposition", "inline; filename=\"" + (arch.nombreOriginal() != null ? arch.nombreOriginal() : "dictamen.pdf") + "\"")
                .build();
    }

    private void guard() {
        securityUtils.requireAnyRole("ESTUDIANTE");
    }
}
