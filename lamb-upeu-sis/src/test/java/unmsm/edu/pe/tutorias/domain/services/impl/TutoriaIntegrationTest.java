package unmsm.edu.pe.tutorias.domain.services.impl;

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
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.tutorias.application.dto.AsignarEnBloqueRequest;
import unmsm.edu.pe.tutorias.application.dto.AsignarEnBloqueResponse;
import unmsm.edu.pe.tutorias.application.dto.TutorComboItem;
import unmsm.edu.pe.tutorias.domain.repositories.TutoriaRepository;
import unmsm.edu.pe.tutorias.domain.services.TutoriaService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integración real (Quarkus + H2) del cambio de tutor con historial y del cupo.
 */
@QuarkusTest
class TutoriaIntegrationTest {

    @Inject TutoriaService service;
    @Inject TutoriaRepository repo;
    @Inject EntityManager em;

    private UUID doc1; // cupo 2
    private UUID doc2; // cupo 5
    private UUID est1;
    private UUID est2;
    private UUID est3;

    @BeforeEach
    void seed() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        QuarkusTransaction.requiringNew().run(() -> {
            doc1 = crearDocente("D1" + s, 2);
            doc2 = crearDocente("D2" + s, 5);
            est1 = crearEstudiante("E1" + s);
            est2 = crearEstudiante("E2" + s);
            est3 = crearEstudiante("E3" + s);
        });
    }

    private UUID crearDocente(String suf, int cupo) {
        Persona p = persona("81" + suf.substring(0, 6));
        em.persist(p);
        Docente d = Docente.builder().persona(p)
                .cupoMaximoTutoria(cupo).build();
        em.persist(d);
        em.flush();
        return d.getPersonaId();
    }

    private UUID crearEstudiante(String suf) {
        Persona p = persona("82" + suf.substring(0, 6));
        em.persist(p);
        Estudiante e = Estudiante.builder().persona(p).build();
        em.persist(e);
        em.flush();
        return e.getPersonaId();
    }

    private Persona persona(String dni) {
        return Persona.builder().tipoDocumento(TipoDocumento.DNI).numeroDocumento(dni)
                .apellidoPaterno("Prueba").nombres("Test").build();
    }

    @Test
    void cambioDeTutor_conservaHistorial_yUnaSolaVigente() {
        service.asignarIndividual(est1, doc1, null);
        service.asignarIndividual(est1, doc2, "Cambio de línea");

        QuarkusTransaction.requiringNew().run(() -> {
            assertEquals(doc2, repo.findVigenteByEstudiante(est1).orElseThrow().getDocente().getPersonaId());
            var hist = repo.findByEstudianteId(est1);
            assertEquals(2, hist.size());
            long vigentes = hist.stream().filter(t -> Boolean.TRUE.equals(t.getActual())).count();
            assertEquals(1, vigentes, "solo una tutoría vigente");
        });
    }

    @Test
    void asignar_mismoTutor_esIdempotente() {
        service.asignarIndividual(est2, doc2, null);
        service.asignarIndividual(est2, doc2, null); // idempotente

        QuarkusTransaction.requiringNew().run(() ->
                assertEquals(1, repo.findByEstudianteId(est2).size()));
    }

    @Test
    void buscarTutores_devuelveCualquierDocente_conCupo() {
        // Cualquier docente activo es candidato a tutor (sin exigir el rol PROF_TUTOR).
        String s = UUID.randomUUID().toString().substring(0, 8);
        UUID[] tutorId = new UUID[1];
        QuarkusTransaction.requiringNew().run(() -> {
            Persona p = Persona.builder().tipoDocumento(TipoDocumento.DNI)
                    .numeroDocumento("83" + s.substring(0, 6)).apellidoPaterno("Zavaleta").nombres("Tutor")
                    .build();
            em.persist(p);
            Docente d = Docente.builder().persona(p)
                    .cupoMaximoTutoria(5).build();
            em.persist(d);
            em.flush();
            tutorId[0] = d.getPersonaId();
        });

        List<TutorComboItem> found = service.buscarTutores("Zavaleta", 0, 10).getContent();
        assertTrue(found.stream().anyMatch(t -> t.getId().equals(tutorId[0])
                && t.getCupoMaximo() == 5 && t.isDisponible()));
    }

    @Test
    void cupo_excedidoEnBloque_yEnIndividual() {
        // doc1 tiene cupo 2 → 3 estudiantes excede.
        assertThrows(BusinessException.class, () -> service.asignarEnBloque(
                new AsignarEnBloqueRequest(doc1, List.of(est1, est2, est3), null)));

        AsignarEnBloqueResponse res = service.asignarEnBloque(
                new AsignarEnBloqueRequest(doc1, List.of(est1, est2), null));
        assertEquals(2, res.getAsignados());
        assertEquals(2, res.getCupoDespues());

        // doc1 ya está lleno (2/2) → individual falla.
        assertThrows(BusinessException.class, () -> service.asignarIndividual(est3, doc1, null));

        QuarkusTransaction.requiringNew().run(() ->
                assertEquals(2, repo.countActualesByDocente(doc1)));
    }
}
