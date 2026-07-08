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
import unmsm.edu.pe.tutorias.domain.entities.Tutoria;
import unmsm.edu.pe.tutorias.domain.services.ReporteTutoresService;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Integración real (Quarkus + H2) del reporte de tutores: consultas + export PDF/Excel. */
@QuarkusTest
class ReporteTutoresIntegrationTest {

    @Inject ReporteTutoresService service;
    @Inject EntityManager em;

    private String suf;
    private UUID tutorId;
    private int seq;

    @BeforeEach
    void seed() {
        suf = UUID.randomUUID().toString().substring(0, 8);
        seq = 0;
        QuarkusTransaction.requiringNew().run(() -> {
            Docente d = docente("Ztut" + suf, 5);
            tutorId = d.getPersonaId();
            UUID e1 = estudiante("CON1" + suf, "Alfa");
            UUID e2 = estudiante("CON2" + suf, "Beta");
            estudiante("SIN" + suf, "Gamma"); // sin tutor
            em.persist(Tutoria.builder().estudiante(em.find(Estudiante.class, e1)).docente(d)
                    .fechaInicio(LocalDate.now()).actual(true).build());
            em.persist(Tutoria.builder().estudiante(em.find(Estudiante.class, e2)).docente(d)
                    .fechaInicio(LocalDate.now()).actual(true).build());
            em.flush();
        });
    }

    private String dni() {
        return "8" + suf + (seq++);
    }

    private Docente docente(String apellido, int cupo) {
        Persona p = Persona.builder().tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(dni())
                .apellidoPaterno(apellido).nombres("Tutor").build();
        em.persist(p);
        Docente d = Docente.builder().persona(p)
                .cupoMaximoTutoria(cupo).build();
        em.persist(d);
        em.flush();
        return d;
    }

    private UUID estudiante(String codigo, String apellido) {
        Persona p = Persona.builder().tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(dni())
                .apellidoPaterno(apellido).nombres("Est").build();
        em.persist(p);
        Estudiante e = Estudiante.builder().persona(p).codigoSistema(codigo).codMatricula("M" + codigo).build();
        em.persist(e);
        em.flush();
        return e.getPersonaId();
    }

    @Test
    void listado_yEstudiantesDeTutor() {
        var page = service.listar(suf, null, null, 0, 10); // filtra por el apellido del tutor (contiene suf)
        assertEquals(1, page.getTotal());
        assertEquals(2, page.getContent().get(0).getEstudiantes());
        assertFalse(page.getContent().get(0).isCupoLleno()); // 2 < 5

        var est = service.estudiantesDeTutor(tutorId, 0, 50);
        assertEquals(2, est.getTotal());
    }

    @Test
    void sinTutor_listaElEstudianteSinTutoria() {
        var sin = service.estudiantesSinTutor(null, null, "SIN" + suf, 0, 10);
        assertEquals(1, sin.getTotal());
        assertEquals("SIN" + suf, sin.getContent().get(0).getCodigoSistema());
    }

    @Test
    void export_generaPdfYExcelNoVacios() {
        byte[] xlsx = service.exportarExcel(suf, null, null);
        byte[] pdf = service.exportarPdf(suf, null, null);
        assertTrue(xlsx.length > 0 && pdf.length > 0);
        // Firmas de formato: XLSX = ZIP ("PK"), PDF = "%PDF"
        assertEquals('P', (char) xlsx[0]);
        assertEquals('K', (char) xlsx[1]);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
    }
}
