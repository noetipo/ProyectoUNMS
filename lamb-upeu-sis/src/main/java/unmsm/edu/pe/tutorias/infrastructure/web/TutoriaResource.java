package unmsm.edu.pe.tutorias.infrastructure.web;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import unmsm.edu.pe.security.infrastructure.utils.SecurityUtils;
import unmsm.edu.pe.shared.response.ApiResponse;
import unmsm.edu.pe.tutorias.application.dto.AsignarEnBloqueRequest;
import unmsm.edu.pe.tutorias.application.dto.AsignarIndividualRequest;
import unmsm.edu.pe.tutorias.domain.services.TutoriaService;

import java.util.UUID;

@Path("/api/tutorias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tutorías", description = "Asignación de tutor académico (individual y en bloque) con historial")
public class TutoriaResource {

    @Inject TutoriaService tutoriaService;
    @Inject SecurityUtils securityUtils;

    @GET
    @Path("/tutores")
    @Operation(summary = "Buscar docentes (candidatos a tutor) con su cupo")
    public Response tutores(
            @QueryParam("buscar") String buscar,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        return Response.ok(ApiResponse.success("Tutores recuperados",
                tutoriaService.buscarTutores(buscar, page, size))).build();
    }

    @GET
    @Path("/estudiantes")
    @Operation(summary = "Estudiantes para asignación (filtros programa + estado tutor + texto)")
    public Response estudiantes(
            @QueryParam("facultadId") String facultadId,
            @QueryParam("programaId") String programaId,
            @QueryParam("conTutor") String conTutor,
            @QueryParam("buscar") String buscar,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        guard();
        UUID fac = (facultadId != null && !facultadId.isBlank()) ? UUID.fromString(facultadId) : null;
        UUID prog = (programaId != null && !programaId.isBlank()) ? UUID.fromString(programaId) : null;
        Boolean con = parseBoolean(conTutor);
        return Response.ok(ApiResponse.success("Estudiantes recuperados",
                tutoriaService.estudiantesAsignables(fac, prog, con, buscar, page, size))).build();
    }

    @POST
    @Path("/asignar")
    @Operation(summary = "Asignar/cambiar tutor de un estudiante (individual)")
    public Response asignar(@Valid AsignarIndividualRequest request) {
        guard();
        tutoriaService.asignarIndividual(request.getEstudianteId(), request.getTutorId(), request.getMotivoCambio());
        return Response.ok(ApiResponse.success("Tutor asignado",
                tutoriaService.tutorVigente(request.getEstudianteId()))).build();
    }

    @POST
    @Path("/asignar-en-bloque")
    @Operation(summary = "Asignar un tutor a varios estudiantes (valida cupo total)")
    public Response asignarEnBloque(@Valid AsignarEnBloqueRequest request) {
        guard();
        return Response.ok(ApiResponse.success("Asignación en bloque aplicada",
                tutoriaService.asignarEnBloque(request))).build();
    }

    @DELETE
    @Path("/{tutorId}/estudiantes/{estudianteId}")
    @Operation(summary = "Quitar un estudiante de un tutor (finaliza la tutoría vigente)")
    public Response quitar(
            @PathParam("tutorId") UUID tutorId,
            @PathParam("estudianteId") UUID estudianteId,
            @QueryParam("motivo") String motivo) {
        guard();
        tutoriaService.finalizar(tutorId, estudianteId, motivo);
        return Response.ok(ApiResponse.success("Estudiante retirado del tutor")).build();
    }

    @GET
    @Path("/estudiante/{id}/vigente")
    @Operation(summary = "Tutor vigente de un estudiante (o null)")
    public Response vigente(@PathParam("id") UUID estudianteId) {
        guard();
        return Response.ok(ApiResponse.success("Tutor vigente",
                tutoriaService.tutorVigente(estudianteId))).build();
    }

    @GET
    @Path("/estudiante/{id}/historial")
    @Operation(summary = "Historial de tutorías de un estudiante")
    public Response historial(@PathParam("id") UUID estudianteId) {
        guard();
        return Response.ok(ApiResponse.success("Historial recuperado",
                tutoriaService.historial(estudianteId))).build();
    }

    private Boolean parseBoolean(String v) {
        if (v == null || v.isBlank() || "null".equalsIgnoreCase(v) || "todos".equalsIgnoreCase(v)) {
            return null;
        }
        return Boolean.parseBoolean(v.trim());
    }

    /** Roles de gestión/edición (pueden asignar/quitar tutorías). */
    private void guard() {
        securityUtils.requireAnyRole("ADMIN", "SECRETARIA", "COORDINADOR", "COORD_PROG", "PERS_ADMIN");
    }
}
