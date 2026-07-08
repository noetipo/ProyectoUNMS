package unmsm.edu.pe.personas.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.personas.application.dto.*;
import unmsm.edu.pe.personas.domain.services.GuardarPerfilCompletoService;
import unmsm.edu.pe.personas.domain.services.PersonaService;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.response.ApiResponse;

import java.util.Set;
import java.util.UUID;

@Path("/api/personas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Personas", description = "Registro de personas y perfiles (estudiante/docente)")
public class PersonaResource {

    @Inject PersonaService personaService;
    @Inject GuardarPerfilCompletoService perfilCompletoService;
    @Inject SecurityUtils securityUtils;
    @Inject ObjectMapper objectMapper;
    @Inject Validator validator;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Registrar persona (multipart)",
            description = "Crea persona + cuenta + perfil(es) + historiales + documentos en una sola petición. " +
                    "Parte 'datos' = JSON (CrearPersonaRequest con 'perfil'); más una parte de archivo por tipo de documento. " +
                    "Exige DNI y PARTIDA_NACIMIENTO.")
    public Response crear(PerfilMultipartForm form) {
        requireSecretariaOrAdmin();
        CrearPersonaRequest request = parseYValida(form.datos, CrearPersonaRequest.class);
        PersonaResponse response = personaService.registrar(request, form.leerArchivos());
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Persona registrada exitosamente", response))
                .build();
    }

    @POST
    @Path("/{id}/perfil-completo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Guardar perfil completo (multipart)",
            description = "Persiste/actualiza historiales de cargos y centros + documentos de la persona")
    public Response perfilCompleto(
            @Parameter(description = "ID de la persona") @PathParam("id") UUID id,
            PerfilMultipartForm form) {
        requireSecretariaOrAdmin();
        PerfilCompletoData data = parse(form.datos, PerfilCompletoData.class);
        PerfilCompletoResponse response = perfilCompletoService.guardar(id, data, form.leerArchivos(), true);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Perfil completo guardado", response))
                .build();
    }

    @GET
    @Path("/{id}/perfil-completo")
    @Operation(summary = "Obtener perfil completo",
            description = "Persona + historiales (cargos/centros) + documentos de la persona")
    public Response obtenerPerfilCompleto(
            @Parameter(description = "ID de la persona") @PathParam("id") UUID id) {
        requireSecretariaOrAdmin();
        PerfilCompletoResponse response = perfilCompletoService.obtenerPerfilCompleto(id);
        return Response.ok(ApiResponse.success("Perfil completo recuperado", response)).build();
    }

    @GET
    @Path("/{id}/documentos/{documentoId}")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar documento de persona",
            description = "Sirve el archivo de un documento de la persona (ADMIN/SECRETARIA)")
    public Response descargarDocumento(
            @Parameter(description = "ID de la persona") @PathParam("id") UUID id,
            @Parameter(description = "ID del documento") @PathParam("documentoId") UUID documentoId) {
        requireSecretariaOrAdmin();
        DocumentoDescarga descarga = perfilCompletoService.descargarDocumento(id, documentoId);
        return archivoResponse(descarga);
    }

    @POST
    @Path("/{id}/perfiles/docente")
    @Operation(summary = "Agregar perfil docente", description = "Agrega el perfil docente a una persona existente")
    public Response agregarPerfilDocente(
            @Parameter(description = "ID de la persona") @PathParam("id") UUID id,
            @Valid AgregarPerfilDocenteRequest request) {
        requireSecretariaOrAdmin();
        PersonaResponse response = personaService.agregarPerfilDocente(id, request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Perfil docente agregado", response))
                .build();
    }

    @GET
    @Operation(summary = "Listar personas", description = "Listado paginado con filtros (ADMIN/SECRETARIA)")
    public Response listar(
            @QueryParam("search") String search,
            @QueryParam("tipoPerfil") String tipoPerfil,
            @QueryParam("activo") Boolean activo,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        requireSecretariaOrAdmin();
        var pageResponse = personaService.listar(search, tipoPerfil, activo, page, size);
        return Response.ok(ApiResponse.success("Personas recuperadas", pageResponse)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Ficha de persona", description = "Datos de la persona, perfiles y roles")
    public Response obtener(@Parameter(description = "ID de la persona") @PathParam("id") UUID id) {
        requireSecretariaOrAdmin();
        PersonaResponse response = personaService.obtener(id);
        return Response.ok(ApiResponse.success("Persona recuperada", response)).build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Actualizar persona", description = "Actualiza datos de persona y de sus perfiles")
    public Response actualizar(
            @Parameter(description = "ID de la persona") @PathParam("id") UUID id,
            @Valid PersonaUpdateRequest request) {
        requireSecretariaOrAdmin();
        PersonaResponse response = personaService.actualizar(id, request);
        return Response.ok(ApiResponse.success("Persona actualizada", response)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar persona (lógico)", description = "Marca la persona como inactiva")
    public Response eliminar(@Parameter(description = "ID de la persona") @PathParam("id") UUID id) {
        requireSecretariaOrAdmin();
        personaService.eliminarLogico(id);
        return Response.ok(ApiResponse.success("Persona desactivada")).build();
    }

    /** Solo SECRETARIA o ADMIN pueden registrar/consultar/modificar personas. */
    private void requireSecretariaOrAdmin() {
        securityUtils.requireAnyRole("ADMIN", "SECRETARIA");
    }

    /** Construye la respuesta HTTP de un archivo (preview inline + nombre original). */
    static Response archivoResponse(DocumentoDescarga descarga) {
        String contentType = descarga.contentType() != null ? descarga.contentType()
                : MediaType.APPLICATION_OCTET_STREAM;
        String nombre = descarga.nombreOriginal() != null ? descarga.nombreOriginal() : "documento";
        return Response.ok(descarga.contenido(), contentType)
                .header("Content-Disposition", "inline; filename=\"" + nombre.replace("\"", "") + "\"")
                .build();
    }

    private <T> T parse(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new BusinessException("JSON inválido en la parte 'datos': " + e.getOriginalMessage());
        }
    }

    private <T> T parseYValida(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new BusinessException("Falta la parte 'datos' (JSON)");
        }
        T obj = parse(json, type);
        Set<ConstraintViolation<T>> violations = validator.validate(obj);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return obj;
    }
}
