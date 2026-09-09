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
import unmsm.edu.pe.tesis.domain.services.SecretariaDefensaService;

import java.util.UUID;

/** Secretaría · Etapa 5 (Defensa) — bandeja de expedientes recibidos y comunicación al Coordinador. */
@Path("/api/secretaria/defensa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Fase 5 · Secretaría (Defensa)", description = "Recepción del expediente y comunicación al Coordinador")
public class SecretariaDefensaResource {

    @Inject SecretariaDefensaService service;
    @Inject SecurityUtils securityUtils;

    @GET
    @Operation(summary = "Bandeja de solicitudes de aprobación recibidas")
    public Response bandeja(@QueryParam("buscar") String buscar,
                            @QueryParam("page") @DefaultValue("0") int page,
                            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja recuperada", service.bandeja(buscar, page, size))).build();
    }

    @POST
    @Path("/{tesisId}/recibir")
    @Operation(summary = "Recibir el expediente y comunicar al Coordinador del Programa")
    public Response recibir(@PathParam("tesisId") UUID tesisId) {
        guard();
        service.recibir(tesisId);
        return Response.ok(ApiResponse.success("Expediente recibido y comunicado al Coordinador")).build();
    }

    @GET
    @Path("/rubricas")
    @Operation(summary = "Proyectos con revisores designados que requieren la rúbrica oficial")
    public Response bandejaRubricas(@QueryParam("buscar") String buscar) {
        guard();
        return Response.ok(ApiResponse.success("Bandeja de rúbricas", service.bandejaRubricas(buscar))).build();
    }

    @GET
    @Path("/{tesisId}/rubrica/preview")
    @Operation(summary = "Vista previa (texto) de la rúbrica subida — para confirmar el documento, no descarga")
    public Response previewRubrica(@PathParam("tesisId") UUID tesisId) {
        guard();
        return Response.ok(ApiResponse.success("Vista previa", service.previsualizarRubrica(tesisId))).build();
    }

    @GET
    @Path("/{tesisId}/rubrica/raw")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "El Word (.docx) crudo de la rúbrica, inline, para renderizarlo en el navegador")
    public Response rubricaRaw(@PathParam("tesisId") UUID tesisId) {
        guard();
        var a = service.documentoRubrica(tesisId);
        String ct = a.contentType() != null ? a.contentType()
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String name = a.nombreOriginal() != null ? a.nombreOriginal() : "rubrica.docx";
        return Response.ok(a.contenido(), ct)
                .header("Content-Disposition", "inline; filename=\"" + name + "\"")
                .build();
    }

    @POST
    @Path("/{tesisId}/rubrica/habilitar")
    @Operation(summary = "Habilitar/suspender la evaluación con la rúbrica oficial del sistema")
    public Response habilitarRubrica(@PathParam("tesisId") UUID tesisId,
                                     @QueryParam("habilitar") @DefaultValue("true") boolean habilitar) {
        guard();
        service.habilitarRubrica(tesisId, habilitar);
        return Response.ok(ApiResponse.success(habilitar
                ? "Evaluación habilitada: los revisores ya pueden evaluar"
                : "Evaluación suspendida")).build();
    }

    @POST
    @Path("/{tesisId}/rubrica")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Subir la rúbrica oficial (Excel) — habilita la evaluación de los revisores")
    public Response subirRubrica(@PathParam("tesisId") UUID tesisId, @RestForm("archivo") FileUpload archivo) {
        guard();
        if (archivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiResponse.error("Adjunta la rúbrica (Excel)")).build();
        }
        try {
            byte[] contenido = java.nio.file.Files.readAllBytes(archivo.uploadedFile());
            service.subirRubrica(tesisId, contenido, archivo.fileName(), archivo.contentType());
            return Response.ok(ApiResponse.success("Rúbrica subida; los revisores ya pueden evaluar")).build();
        } catch (java.io.IOException e) {
            throw new BusinessException("No se pudo leer el archivo subido");
        }
    }

    // ── Programación de la defensa (la realiza la Secretaría) ──

    @GET
    @Path("/{tesisId}/defensa")
    @Operation(summary = "Info de la defensa: programada, fecha/hora/lugar/modalidad y quiénes la evalúan")
    public Response defensa(@PathParam("tesisId") UUID tesisId) {
        guard();
        return Response.ok(ApiResponse.success("Defensa", service.defensa(tesisId))).build();
    }

    @POST
    @Path("/{tesisId}/defensa")
    @Operation(summary = "Programar la defensa: modalidad, fecha, hora y aula/enlace")
    public Response programarDefensa(@PathParam("tesisId") UUID tesisId,
                                     unmsm.edu.pe.tesis.application.dto.ProgramarDefensaRequest req) {
        guard();
        service.programarDefensa(tesisId, req);
        return Response.ok(ApiResponse.success("Defensa programada")).build();
    }

    private void guard() {
        securityUtils.requireAnyRole("SECRETARIA", "ADMIN");
    }
}
