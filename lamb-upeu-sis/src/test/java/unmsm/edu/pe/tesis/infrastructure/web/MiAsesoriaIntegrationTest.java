package unmsm.edu.pe.tesis.infrastructure.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unmsm.edu.pe.personas.domain.entities.*;
import unmsm.edu.pe.personas.domain.enums.GradoAcademico;
import unmsm.edu.pe.personas.domain.enums.NivelPrograma;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.tesis.domain.entities.SugerenciaAsesor;
import unmsm.edu.pe.tesis.domain.entities.Tesis;
import unmsm.edu.pe.tesis.domain.entities.TesisAutor;
import unmsm.edu.pe.tesis.domain.enums.EstadoTesis;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Ciclo completo de la bandeja del estudiante: ve su tema + asesores sugeridos,
 * solicita asesoría (Solicitud PDF), el asesor acepta (crea Asesoria + Carta PDF),
 * el estado derivado pasa a aceptado, y valida propiedad de los documentos.
 */
@QuarkusTest
class MiAsesoriaIntegrationTest {

    private static final String SECRET = "mySecretKey1234567890abcdefghij";

    @Inject EntityManager em;

    private String suf;
    private UUID estUserId, docUserId, otroEstUserId;
    private UUID asesorDocenteId, lineaId;

    @BeforeEach
    void seed() {
        suf = UUID.randomUUID().toString().substring(0, 8);
        QuarkusTransaction.requiringNew().run(() -> {
            ProgramaPosgrado prog = ProgramaPosgrado.builder()
                    .nombre("Maestría asesoría " + suf).nivel(NivelPrograma.MAESTRIA).build();
            em.persist(prog);

            LineaInvestigacion linea = LineaInvestigacion.builder().nombre("Línea asesoría " + suf).build();
            em.persist(linea);
            lineaId = linea.getId();

            // Estudiante con usuario (para token)
            User estUser = user("est-" + suf);
            estUserId = estUser.getId();
            Persona estP = persona("Estudiante", "Uno", estUser);
            Estudiante est = Estudiante.builder().persona(estP).programa(prog)
                    .codigoSistema("EST-" + suf).codMatricula("M" + suf).build();
            em.persist(est);

            // Docente asesor con usuario + su línea (requisito de crear solicitud)
            User docUser = user("doc-" + suf);
            docUserId = docUser.getId();
            Persona docP = persona("Asesor", "Docente", docUser);
            Docente asesor = Docente.builder().persona(docP)
                    .emailInstitucional("asesor-" + suf + "@unmsm.edu.pe").build();
            em.persist(asesor);
            asesorDocenteId = docP.getId();
            em.persist(DocenteLineaInvestigacion.builder()
                    .docente(asesor).lineaInvestigacion(linea).esPrincipal(true).build());

            // Tutor (para la sugerencia)
            Persona tutP = persona("Tutor", "Demo", null);
            Docente tutor = Docente.builder().persona(tutP).build();
            em.persist(tutor);

            // Tesis activa del estudiante (tema registrado, sin asesor aún)
            Tesis tesis = Tesis.builder().titulo("Tema demo " + suf).lineaInvestigacion(linea)
                    .nivel(NivelPrograma.MAESTRIA).estado(EstadoTesis.TEMA_REGISTRADO)
                    .fechaRegistro(LocalDate.now()).build();
            em.persist(tesis);
            em.persist(TesisAutor.builder().tesisId(tesis.getId()).estudianteId(estP.getId()).esActiva(true).build());

            // Sugerencia del tutor: asesor sugerido al estudiante
            em.persist(SugerenciaAsesor.builder().estudianteId(estP.getId())
                    .tutorId(tutP.getId()).asesorDocenteId(docP.getId())
                    .nota("Afinidad de línea").build());

            // Otro estudiante (para validar propiedad de documentos)
            User otroUser = user("otro-" + suf);
            otroEstUserId = otroUser.getId();
            Persona otroP = persona("Otro", "Estudiante", otroUser);
            em.persist(Estudiante.builder().persona(otroP).programa(prog)
                    .codigoSistema("EST2-" + suf).codMatricula("M2" + suf).build());

            em.flush();
        });
    }

    private int seq;

    private User user(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@unmsm.edu.pe");
        u.setPassword("x");
        u.setStatus(UserStatus.ACTIVE);
        em.persist(u);
        return u;
    }

    private Persona persona(String apPat, String nombres, User user) {
        Persona p = Persona.builder().tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("6" + suf + (seq++))
                .apellidoPaterno(apPat).nombres(nombres).user(user).build();
        em.persist(p);
        return p;
    }

    private String token(UUID userId, String role) {
        try {
            long now = Instant.now().getEpochSecond();
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payload = "{\"iss\":\"https://unmsm.edu.pe\",\"aud\":\"upeu-sis\",\"sub\":\"tester\","
                    + "\"userId\":\"" + userId + "\",\"roles\":[\"" + role + "\"],"
                    + "\"exp\":" + (now + 3600) + ",\"iat\":" + now + "}";
            String eh = b64(header), ep = b64(payload);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sig = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((eh + "." + ep).getBytes(StandardCharsets.UTF_8)));
            return "Bearer " + eh + "." + ep + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void cicloCompletoDeAsesoria() {
        String est = token(estUserId, "ESTUDIANTE");
        String doc = token(docUserId, "DOCENTE");

        // Bandeja inicial: tema, un asesor sugerido, sin solicitud, sin documentos.
        given().header("Authorization", est).when().get("/api/mi-asesoria")
                .then().statusCode(200)
                .body("data.conTema", is(true))
                .body("data.estadoDerivado", is("SIN_ASESOR"))
                .body("data.sugeridos.size()", is(1))
                .body("data.sugeridos[0].asesorDocenteId", is(asesorDocenteId.toString()))
                .body("data.solicitudEstado", is("SIN_SOLICITUD"))
                .body("data.solicitudPdfDisponible", is(false));

        // Solicita asesoría al asesor sugerido.
        String solicitudBody = "{\"docenteId\":\"" + asesorDocenteId + "\",\"lineaInvestigacionId\":\"" + lineaId
                + "\",\"tituloTentativo\":\"Tema demo\",\"tipo\":\"ASESOR\"}";
        String solicitudId = given().header("Authorization", est).contentType("application/json").body(solicitudBody)
                .when().post("/api/solicitudes-asesoria")
                .then().statusCode(201).extract().path("data.id");

        // Solicitud PENDIENTE: los firmados son un paquete post-aceptación, así que
        // ni la Solicitud ni la Carta están disponibles todavía.
        given().header("Authorization", est).when().get("/api/mi-asesoria")
                .then().statusCode(200)
                .body("data.solicitudEstado", is("PENDIENTE"))
                .body("data.solicitudPdfDisponible", is(false))
                .body("data.cartaPdfDisponible", is(false));

        // Con la solicitud aún PENDIENTE, descargar la Solicitud o la Carta falla (409).
        given().header("Authorization", est)
                .when().get("/api/mi-asesoria/documentos/SOLICITUD_ASESORIA")
                .then().statusCode(409);
        given().header("Authorization", est)
                .when().get("/api/mi-asesoria/documentos/CARTA_ACEPTACION")
                .then().statusCode(409);

        // El asesor acepta → crea Asesoria + estado derivado pasa a TEMA_REGISTRADO.
        given().header("Authorization", doc).contentType("application/json").body("{\"decision\":\"ACEPTAR\"}")
                .when().post("/api/solicitudes-asesoria/" + solicitudId + "/responder")
                .then().statusCode(200);

        // Tras aceptar, AMBOS documentos quedan disponibles para firma.
        given().header("Authorization", est).when().get("/api/mi-asesoria")
                .then().statusCode(200)
                .body("data.solicitudEstado", is("ACEPTADA"))
                .body("data.estadoDerivado", is("TEMA_REGISTRADO"))
                .body("data.solicitudPdfDisponible", is(true))
                .body("data.cartaPdfDisponible", is(true));

        // Descarga la Solicitud PDF (magic bytes %PDF).
        byte[] solicitudPdf = given().header("Authorization", est)
                .when().get("/api/mi-asesoria/documentos/SOLICITUD_ASESORIA")
                .then().statusCode(200).contentType("application/pdf").extract().asByteArray();
        assert solicitudPdf.length > 4 && solicitudPdf[0] == '%' && solicitudPdf[1] == 'P'
                && solicitudPdf[2] == 'D' && solicitudPdf[3] == 'F';

        // Descarga la Carta de aceptación PDF.
        byte[] cartaPdf = given().header("Authorization", est)
                .when().get("/api/mi-asesoria/documentos/CARTA_ACEPTACION")
                .then().statusCode(200).contentType("application/pdf").extract().asByteArray();
        assert cartaPdf.length > 4 && cartaPdf[0] == '%' && cartaPdf[1] == 'P';
    }

    @Test
    void otroEstudianteNoVeDocumentosAjenos() {
        // El otro estudiante no tiene solicitud → su descarga de Solicitud falla (409),
        // nunca obtiene el documento del primer estudiante.
        given().header("Authorization", token(otroEstUserId, "ESTUDIANTE"))
                .when().get("/api/mi-asesoria/documentos/SOLICITUD_ASESORIA")
                .then().statusCode(409);

        given().header("Authorization", token(otroEstUserId, "ESTUDIANTE"))
                .when().get("/api/mi-asesoria")
                .then().statusCode(200)
                .body("data.conTema", is(false))
                .body("data.solicitudEstado", is("SIN_SOLICITUD"));
    }
}
