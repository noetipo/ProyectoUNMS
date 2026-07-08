package unmsm.edu.pe.personas.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.personas.application.dto.DocumentoDescarga;
import unmsm.edu.pe.personas.application.dto.MiPerfilUpdateRequest;
import unmsm.edu.pe.personas.application.dto.PerfilCompletoData;
import unmsm.edu.pe.personas.domain.services.MiPerfilService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.response.ApiResponse;

@Path("/api/mi-perfil")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mi Perfil", description = "Perfil del usuario autenticado")
public class MiPerfilResource {

    @Inject MiPerfilService miPerfilService;
    @Inject ObjectMapper objectMapper;

    @GET
    @Operation(summary = "Mi perfil", description = "Persona y perfiles del usuario autenticado")
    public Response obtener() {
        return Response.ok(ApiResponse.success("Perfil recuperado", miPerfilService.obtenerMiPerfil())).build();
    }

    @PUT
    @Operation(summary = "Actualizar mi perfil", description = "Edita solo campos permitidos (celular, email personal, ORCID)")
    public Response actualizar(@Valid MiPerfilUpdateRequest request) {
        return Response.ok(ApiResponse.success("Perfil actualizado", miPerfilService.actualizarMiPerfil(request))).build();
    }

    @GET
    @Path("/completo")
    @Operation(summary = "Mi perfil completo", description = "Persona + historiales (cargos/centros) + documentos")
    public Response obtenerCompleto() {
        return Response.ok(ApiResponse.success("Perfil completo recuperado", miPerfilService.obtenerMiPerfilCompleto())).build();
    }

    @PUT
    @Path("/completo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Actualizar mi perfil completo (multipart)",
            description = "Actualiza mis cargos, centros y documentos (persona del token)")
    public Response actualizarCompleto(PerfilMultipartForm form) {
        PerfilCompletoData data = parse(form.datos);
        return Response.ok(ApiResponse.success("Perfil completo actualizado",
                miPerfilService.guardarMiPerfilCompleto(data, form.leerArchivos()))).build();
    }

    @GET
    @Path("/documentos/{documentoId}")
    @Produces(MediaType.WILDCARD)
    @Operation(summary = "Descargar un documento propio",
            description = "Sirve un documento de la persona del token; nunca el de otra persona")
    public Response descargarDocumento(@PathParam("documentoId") java.util.UUID documentoId) {
        DocumentoDescarga descarga = miPerfilService.descargarMiDocumento(documentoId);
        return PersonaResource.archivoResponse(descarga);
    }

    private PerfilCompletoData parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PerfilCompletoData.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException("JSON inválido en la parte 'datos': " + e.getOriginalMessage());
        }
    }
}
