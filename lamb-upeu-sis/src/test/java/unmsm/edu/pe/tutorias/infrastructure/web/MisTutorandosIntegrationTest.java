package unmsm.edu.pe.tutorias.infrastructure.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.Estudiante;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.enums.GradoAcademico;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.security.domain.entities.User;
import unmsm.edu.pe.security.domain.enums.UserStatus;
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;

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
 * Integración HTTP del panel "Mis tutorandos": el tutor autenticado ve solo sus
 * tutorandos vigentes (aislamiento), y el panel exige rol PROF_TUTOR.
 */
@QuarkusTest
class MisTutorandosIntegrationTest {

    private static final String SECRET = "mySecretKey1234567890abcdefghij";

    @Inject EntityManager em;

    private String suf;
    private int seq;
    private UUID tutorUserId;

    @BeforeEach
    void seed() {
        suf = UUID.randomUUID().toString().substring(0, 8);
        seq = 0;
        QuarkusTransaction.requiringNew().run(() -> {
            User u = new User();
            u.setUsername("mt-" + suf);
            u.setEmail("mt-" + suf + "@unmsm.edu.pe");
            u.setPassword("x");
            u.setStatus(UserStatus.ACTIVE);
            em.persist(u);
            tutorUserId = u.getId();

            Docente tutor = docente("Tutorcito", u);
            asignar(estudiante(), tutor);
            asignar(estudiante(), tutor);

            // Otro tutor con su propio estudiante (no debe verse)
            Docente otro = docente("Otro", null);
            asignar(estudiante(), otro);
            em.flush();
        });
    }

    private String dni() {
        return "8" + suf + (seq++);
    }

    private Docente docente(String apellido, User user) {
        Persona p = Persona.builder().tipoDocumento(TipoDocumento.DNI).numeroDocumento(dni())
                .apellidoPaterno(apellido).nombres("Doc").user(user).build();
        em.persist(p);
        Docente d = Docente.builder().persona(p).cupoMaximoTutoria(20).build();
        em.persist(d);
        em.flush();
        return d;
    }

    private Estudiante estudiante() {
        Persona p = Persona.builder().tipoDocumento(TipoDocumento.DNI).numeroDocumento(dni())
                .apellidoPaterno("Est").nombres("Tutorando").build();
        em.persist(p);
        Estudiante e = Estudiante.builder().persona(p).codMatricula("M" + dni()).build();
        em.persist(e);
        em.flush();
        return e;
    }

    private void asignar(Estudiante e, Docente d) {
        em.persist(Tutoria.builder().estudiante(e).docente(d).fechaInicio(LocalDate.now()).actual(true).build());
    }

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
            return eh + "." + ep + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void tutor_veSoloSusTutorandos() {
        String tok = "Bearer " + token(tutorUserId, "PROF_TUTOR");
        given().header("Authorization", tok)
                .when().get("/api/mis-tutorandos")
                .then().statusCode(200)
                .body("data.total", is(2)); // solo los 2 suyos, no el del otro tutor

        given().header("Authorization", tok)
                .when().get("/api/mis-tutorandos/resumen")
                .then().statusCode(200)
                .body("data.total", is(2))
                .body("data.cupoMaximo", is(20))
                .body("data.tutorNombre", containsStringIgnoringCase("Tutorcito"));
    }

    @Test
    void sinRolTutor_esRechazado() {
        given().header("Authorization", "Bearer " + token(tutorUserId, "ESTUDIANTE"))
                .when().get("/api/mis-tutorandos")
                .then().statusCode(anyOf(is(403), is(500)));
    }
}
