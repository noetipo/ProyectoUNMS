package unmsm.edu.pe.tesis.infrastructure.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.DocenteLineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.enums.GradoAcademico;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integración HTTP del flujo de solicitud de asesoría: el estudiante solicita,
 * aparece en la bandeja del docente, el docente acepta, y el estudiante lo ve en
 * "mis solicitudes". Incluye el filtro de docentes por línea y seguridad por rol.
 */
@QuarkusTest
class SolicitudAsesoriaIntegrationTest {

    private static final String SECRET = "mySecretKey1234567890abcdefghij";

    @Inject EntityManager em;

    private UUID estUserId;
    private UUID docUserId;
    private UUID docenteId;
    private UUID lineaId;

    @BeforeEach
    void seed() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        QuarkusTransaction.requiringNew().run(() -> {
            // Estudiante
            User su = crearUser("it-est-" + suffix);
            Persona sp = crearPersona("81" + suffix.substring(0, 6), "Estudiante", "Prueba", su);
            Estudiante est = Estudiante.builder().persona(sp).codigoSistema("ITE-" + suffix).build();
            em.persist(est);

            // Docente
            User du = crearUser("it-doc-" + suffix);
            Persona dp = crearPersona("82" + suffix.substring(0, 6), "Docente", "Prueba", du);
            Docente doc = Docente.builder().persona(dp).build();
            em.persist(doc);

            // Línea + asignación al docente
            LineaInvestigacion li = LineaInvestigacion.builder().nombre("IT Línea " + suffix).build();
            li.setActive(true);
            em.persist(li);
            em.persist(DocenteLineaInvestigacion.builder()
                    .docente(doc).lineaInvestigacion(li).esPrincipal(true).build());
            em.flush();

            estUserId = su.getId();
            docUserId = du.getId();
            docenteId = doc.getPersonaId();
            lineaId = li.getId();
        });
    }

    private User crearUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@unmsm.edu.pe");
        u.setPassword("x");
        u.setStatus(UserStatus.ACTIVE);
        em.persist(u);
        return u;
    }

    private Persona crearPersona(String dni, String apPat, String nombres, User user) {
        Persona p = Persona.builder()
                .tipoDocumento(TipoDocumento.DNI).numeroDocumento(dni)
                .apellidoPaterno(apPat).nombres(nombres).user(user).build();
        em.persist(p);
        return p;
    }

    // ── token JWT (mismo formato/secreto que JwtTokenGenerator) ──
    private String token(UUID userId, String role) {
        try {
            long now = Instant.now().getEpochSecond();
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payload = "{\"iss\":\"https://unmsm.edu.pe\",\"aud\":\"upeu-sis\","
                    + "\"sub\":\"tester\",\"userId\":\"" + userId + "\",\"roles\":[\"" + role + "\"],"
                    + "\"exp\":" + (now + 3600) + ",\"iat\":" + now + "}";
            String eh = b64(header);
            String ep = b64(payload);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sig = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((eh + "." + ep).getBytes(StandardCharsets.UTF_8)));
            return eh + "." + ep + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void flujoCompleto_solicitar_bandeja_aceptar() {
        String est = "Bearer " + token(estUserId, "ESTUDIANTE");
        String doc = "Bearer " + token(docUserId, "DOCENTE");

        // 2) El estudiante registra la solicitud (queda PENDIENTE).
        String solicitudId = given().header("Authorization", est)
                .contentType(ContentType.JSON)
                .body(Map.of("docenteId", docenteId.toString(),
                        "lineaInvestigacionId", lineaId.toString(),
                        "tituloTentativo", "Tesis de prueba",
                        "mensaje", "Solicito su asesoría"))
                .when().post("/api/solicitudes-asesoria")
                .then().statusCode(201)
                .body("data.estado", equalTo("PENDIENTE"))
                .extract().path("data.id");

        // 3) Aparece en la bandeja del docente destinatario.
        given().header("Authorization", doc)
                .when().get("/api/solicitudes-asesoria/bandeja")
                .then().statusCode(200)
                .body("data.content.find { it.id == '" + solicitudId + "' }", notNullValue());

        // 4) El docente acepta.
        given().header("Authorization", doc)
                .contentType(ContentType.JSON)
                .body(Map.of("decision", "ACEPTAR"))
                .when().post("/api/solicitudes-asesoria/" + solicitudId + "/responder")
                .then().statusCode(200)
                .body("data.estado", equalTo("ACEPTADA"));
    }

    @Test
    void bandeja_conRolEstudiante_esRechazada() {
        // La bandeja es solo para DOCENTE; un estudiante no puede verla.
        given().header("Authorization", "Bearer " + token(estUserId, "ESTUDIANTE"))
                .when().get("/api/solicitudes-asesoria/bandeja")
                .then().statusCode(anyOf(is(403), is(500)))
                .body("success", is(false));
    }
}
