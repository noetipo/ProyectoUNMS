package unmsm.edu.pe.tesis.infrastructure.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.entities.ProgramaPosgrado;
import unmsm.edu.pe.personas.domain.enums.NivelPrograma;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integración HTTP del flujo del coordinador: registrar tema crea la tesis (nivel
 * derivado, estado inicial, estado derivado SIN_ASESOR), no permite duplicar tesis
 * activa, el reporte refleja con/sin tema, y el perfil del estudiante ve su tema.
 */
@QuarkusTest
class CoordinadorTemaIntegrationTest {

    private static final String SECRET = "mySecretKey1234567890abcdefghij";

    @Inject EntityManager em;

    private String suf;
    private UUID estId;
    private UUID lineaId;
    private UUID estUserId;
    private String codMatricula;

    @BeforeEach
    void seed() {
        suf = UUID.randomUUID().toString().substring(0, 8);
        codMatricula = "MTEMA" + suf;
        QuarkusTransaction.requiringNew().run(() -> {
            ProgramaPosgrado prog = ProgramaPosgrado.builder()
                    .nombre("Maestría demo " + suf).nivel(NivelPrograma.MAESTRIA).build();
            em.persist(prog);

            LineaInvestigacion li = LineaInvestigacion.builder()
                    .nombre("Línea tema " + suf).build();
            em.persist(li);
            lineaId = li.getId();

            User u = new User();
            u.setUsername("est-" + suf);
            u.setEmail("est-" + suf + "@unmsm.edu.pe");
            u.setPassword("x");
            u.setStatus(UserStatus.ACTIVE);
            em.persist(u);
            estUserId = u.getId();

            Persona p = Persona.builder().tipoDocumento(TipoDocumento.DNI).numeroDocumento("7" + suf)
                    .apellidoPaterno("Tema").nombres("Estudiante").user(u).build();
            em.persist(p);

            Estudiante e = Estudiante.builder().persona(p).codMatricula(codMatricula)
                    .codigoSistema("EST-" + suf).programa(prog).build();
            em.persist(e);
            em.flush();
            estId = p.getId();
        });
    }

    // ── tokens ──
    private String token(UUID userId, String role) {
        try {
            long now = Instant.now().getEpochSecond();
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payload = "{\"iss\":\"https://unmsm.edu.pe\",\"aud\":\"upeu-sis\",\"sub\":\"tester\","
                    + "\"userId\":\"" + userId + "\",\"roles\":[\"" + role + "\"],"
                    + "\"exp\":" + (now + 3600) + ",\"iat\":" + now + "}";
            String eh = b64(header);
            String ep = b64(payload);
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

    private String body(UUID lineaId, String titulo) {
        return "{\"lineaInvestigacionId\":\"" + lineaId + "\",\"titulo\":\"" + titulo + "\"}";
    }

    @Test
    void registrarTema_creaTesis_conNivelYEstadoDerivado() {
        String coord = token(UUID.randomUUID(), "COORD_PROG");

        // Antes: aparece SIN tema en el reporte.
        given().header("Authorization", coord)
                .when().get("/api/coordinador/estudiantes-tema?buscar=" + codMatricula + "&conTema=false")
                .then().statusCode(200).body("data.total", greaterThanOrEqualTo(1));

        // Registrar el tema → 201, nivel derivado MAESTRIA, estado TEMA_REGISTRADO, derivado SIN_ASESOR.
        given().header("Authorization", coord).contentType("application/json")
                .body(body(lineaId, "Mi tema de tesis"))
                .when().post("/api/coordinador/estudiantes/" + estId + "/tema")
                .then().statusCode(201)
                .body("data.nivel", equalTo("MAESTRIA"))
                .body("data.estado", equalTo("TEMA_REGISTRADO"))
                .body("data.estadoDerivado", equalTo("SIN_ASESOR"))
                .body("data.titulo", equalTo("Mi tema de tesis"));

        // No permite duplicar tesis activa → 409.
        given().header("Authorization", coord).contentType("application/json")
                .body(body(lineaId, "Otro tema"))
                .when().post("/api/coordinador/estudiantes/" + estId + "/tema")
                .then().statusCode(409);

        // Ahora aparece CON tema en el reporte.
        given().header("Authorization", coord)
                .when().get("/api/coordinador/estudiantes-tema?buscar=" + codMatricula + "&conTema=true")
                .then().statusCode(200)
                .body("data.total", equalTo(1))
                .body("data.content[0].conTema", equalTo(true))
                .body("data.content[0].estadoDerivado", equalTo("SIN_ASESOR"));

        // Editar el tema → 200.
        given().header("Authorization", coord).contentType("application/json")
                .body(body(lineaId, "Tema editado"))
                .when().put("/api/coordinador/estudiantes/" + estId + "/tema")
                .then().statusCode(200).body("data.titulo", equalTo("Tema editado"));

        // Perfil del estudiante: ve su tema (mismo estado derivado).
        given().header("Authorization", token(estUserId, "ESTUDIANTE"))
                .when().get("/api/mi-perfil/tema")
                .then().statusCode(200)
                .body("data.titulo", equalTo("Tema editado"))
                .body("data.estadoDerivado", equalTo("SIN_ASESOR"));
    }

    @Test
    void registrarTema_sinRolCoordinador_esRechazado() {
        given().header("Authorization", token(UUID.randomUUID(), "ESTUDIANTE")).contentType("application/json")
                .body(body(lineaId, "No permitido"))
                .when().post("/api/coordinador/estudiantes/" + estId + "/tema")
                .then().statusCode(anyOf(is(403), is(500)));
    }
}
