package unmsm.edu.pe.personas.domain.services.impl;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import unmsm.edu.pe.personas.application.dto.DocenteLineaRequest;
import unmsm.edu.pe.personas.domain.entities.Docente;
import unmsm.edu.pe.personas.domain.entities.LineaInvestigacion;
import unmsm.edu.pe.personas.domain.entities.Persona;
import unmsm.edu.pe.personas.domain.enums.GradoAcademico;
import unmsm.edu.pe.personas.domain.enums.TipoDocumento;
import unmsm.edu.pe.personas.domain.repositories.DocenteLineaInvestigacionRepository;
import unmsm.edu.pe.personas.domain.services.GuardarLineasInvestigacionService;
import unmsm.edu.pe.shared.exceptions.BusinessException;
import unmsm.edu.pe.shared.exceptions.ValidationException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integración real (Quarkus + H2 en memoria) del guardado de líneas de
 * investigación del docente: persistencia efectiva, reemplazo idempotente y
 * validaciones sobre la base de datos, dentro de transacciones reales.
 */
@QuarkusTest
class DocenteLineaInvestigacionIntegrationTest {

    @Inject GuardarLineasInvestigacionService service;
    @Inject DocenteLineaInvestigacionRepository docenteLineaRepository;
    @Inject EntityManager em;

    private UUID docenteId;
    private UUID l1;
    private UUID l2;
    private UUID l3;
    private UUID lInactiva;

    @BeforeEach
    void seed() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        QuarkusTransaction.requiringNew().run(() -> {
            Persona p = Persona.builder()
                    .tipoDocumento(TipoDocumento.DNI)
                    .numeroDocumento("DOC-" + suffix)
                    .apellidoPaterno("Prueba")
                    .nombres("Docente")
                    .build();
            em.persist(p);

            Docente d = Docente.builder()
                    .persona(p)
                    
                    .build();
            em.persist(d);
            docenteId = d.getPersonaId();

            l1 = persistLinea("Inteligencia Artificial " + suffix, true);
            l2 = persistLinea("Ciencia de Datos " + suffix, true);
            l3 = persistLinea("Redes Neuronales " + suffix, true);
            lInactiva = persistLinea("Línea Inactiva " + suffix, false);
            em.flush();
        });
    }

    private UUID persistLinea(String nombre, boolean activa) {
        LineaInvestigacion l = LineaInvestigacion.builder().nombre(nombre).build();
        l.setActive(activa);
        em.persist(l);
        return l.getId();
    }

    private void aplicar(List<DocenteLineaRequest> reqs) {
        QuarkusTransaction.requiringNew().run(() -> {
            Docente d = em.find(Docente.class, docenteId);
            service.aplicar(d, reqs);
        });
    }

    /** Devuelve "lineaId:esPrincipal" ordenado, leído en una transacción propia. */
    private List<String> leer() {
        return QuarkusTransaction.requiringNew().call(() ->
                docenteLineaRepository.findByDocenteId(docenteId).stream()
                        .map(x -> x.getLineaInvestigacion().getId() + ":" + x.getEsPrincipal())
                        .sorted()
                        .collect(Collectors.toList()));
    }

    @Test
    void aplicar_persisteLineasEnBd() {
        aplicar(List.of(new DocenteLineaRequest(l1, true), new DocenteLineaRequest(l2, false)));

        List<String> filas = leer();
        assertEquals(2, filas.size());
        assertTrue(filas.contains(l1 + ":true"));
        assertTrue(filas.contains(l2 + ":false"));
    }

    @Test
    void reAplicar_reemplazaIdempotente() {
        aplicar(List.of(new DocenteLineaRequest(l1, true), new DocenteLineaRequest(l2, false)));
        // Edición: quita l1, mantiene l2 (ahora principal) y agrega l3.
        aplicar(List.of(new DocenteLineaRequest(l2, true), new DocenteLineaRequest(l3, false)));

        List<String> filas = leer();
        assertEquals(2, filas.size());
        assertTrue(filas.contains(l2 + ":true"));
        assertTrue(filas.contains(l3 + ":false"));
        assertFalse(filas.stream().anyMatch(f -> f.startsWith(l1.toString())));
    }

    @Test
    void aplicarListaVacia_eliminaTodas() {
        aplicar(List.of(new DocenteLineaRequest(l1, true)));
        assertEquals(1, leer().size());

        aplicar(List.of());
        assertTrue(leer().isEmpty());
    }

    @Test
    void aplicarNull_noModificaLasExistentes() {
        aplicar(List.of(new DocenteLineaRequest(l1, true)));
        aplicar(null);
        assertEquals(1, leer().size());
    }

    @Test
    void aplicar_dosPrincipales_lanzaValidationYNoPersiste() {
        assertThrows(ValidationException.class, () -> aplicar(List.of(
                new DocenteLineaRequest(l1, true), new DocenteLineaRequest(l2, true))));
        assertTrue(leer().isEmpty());
    }

    @Test
    void aplicar_lineaInactiva_lanzaBusinessYNoPersiste() {
        assertThrows(BusinessException.class,
                () -> aplicar(List.of(new DocenteLineaRequest(lInactiva, false))));
        assertTrue(leer().isEmpty());
    }
}
