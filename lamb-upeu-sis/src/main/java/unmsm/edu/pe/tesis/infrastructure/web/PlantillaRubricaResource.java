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
import unmsm.edu.pe.tesis.domain.services.PlantillaRubricaService;

import java.util.UUID;

/**
 * Rúbricas oficiales de los revisores (Configuración). Se publican una vez —y se actualizan cuando
 * la UPG cambia el documento—, en lugar de adjuntarlas en cada expediente.
 */
@Path("/api/secretaria/rubricas-oficiales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Configuración · Rúbricas oficiales", description = "Plantillas de rúbrica por enfoque, versionadas por año")
public class PlantillaRubricaResource {

    @Inject PlantillaRubricaService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Las dos rúbricas oficiales con su versión vigente e historial")
    public Response listar() {
        lectura();
        return Response.ok(ApiResponse.success("Rúbricas oficiales", service.listar())).build();
    }

    @POST
    @Path("/{enfoque}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Publicar una versión nueva de la rúbrica del enfoque (Word .docx)")
    public Response publicar(@PathParam("enfoque") String enfoque,
                             @RestForm("archivo") FileUpload archivo,
                             @RestForm("version") String version) {
        escritura();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Adjunta la rúbrica en Word (.docx)")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            var item = service.publicar(enfoque, version, contenido, archivo.fileName(), archivo.contentType());
            return Response.ok(ApiResponse.success("Rúbrica publicada", item)).build();
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    @GET
    @Path("/{id}/documento")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Word de una versión concreta (vista previa/descarga)")
    public Response documento(@PathParam("id") UUID id) {
        lectura();
        var a = service.documento(id);
        return Response.ok(a.contenido(), a.contentType())
                .header("Content-Disposition", "inline; filename=\"" + a.nombreOriginal() + "\"")
                .build();
    }

    @GET
    @Path("/vigente/{enfoque}/documento")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Word de la rúbrica vigente del enfoque")
    public Response documentoVigente(@PathParam("enfoque") String enfoque) {
        lectura();
        var a = service.documentoVigente(enfoque);
        return Response.ok(a.contenido(), a.contentType())
                .header("Content-Disposition", "inline; filename=\"" + a.nombreOriginal() + "\"")
                .build();
    }

    /** Consultarlas es útil para quien acompaña el proceso; publicarlas, no. */
    private void lectura() {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN", "COORDINADOR", "COORD_PROG", "REVISOR");
    }

    private void escritura() {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN");
    }
}
