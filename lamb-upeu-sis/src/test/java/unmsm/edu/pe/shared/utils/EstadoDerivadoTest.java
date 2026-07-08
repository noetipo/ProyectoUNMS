package unmsm.edu.pe.shared.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Única fuente de verdad del estado derivado: Sin tema → Sin asesor → estado tesis. */
class EstadoDerivadoTest {

    private final UUID tesis = UUID.randomUUID();

    @Test
    void sinTesis_esSinTema() {
        assertEquals(EstadoDerivado.SIN_TEMA, EstadoDerivado.resolver(null, null, false));
        assertEquals(EstadoDerivado.SIN_TEMA, EstadoDerivado.resolver(null, "TEMA_REGISTRADO", true));
    }

    @Test
    void conTesisSinAsesor_esSinAsesor() {
        assertEquals(EstadoDerivado.SIN_ASESOR, EstadoDerivado.resolver(tesis, null, false));
        assertEquals(EstadoDerivado.SIN_ASESOR, EstadoDerivado.resolver(tesis, "TEMA_REGISTRADO", false));
    }

    @Test
    void conTesisYAsesor_devuelveEstadoTesis() {
        assertEquals("TEMA_REGISTRADO", EstadoDerivado.resolver(tesis, "TEMA_REGISTRADO", true));
        assertEquals("PROYECTO_PRESENTADO", EstadoDerivado.resolver(tesis, "PROYECTO_PRESENTADO", true));
    }

    @Test
    void conTesisYAsesorPeroEstadoNull_caeASinAsesor() {
        assertEquals(EstadoDerivado.SIN_ASESOR, EstadoDerivado.resolver(tesis, null, true));
    }

    @Test
    void etiquetasLegibles() {
        assertEquals("Sin tema", EstadoDerivado.etiqueta(EstadoDerivado.SIN_TEMA));
        assertEquals("Sin asesor", EstadoDerivado.etiqueta(EstadoDerivado.SIN_ASESOR));
        assertEquals("Tema registrado", EstadoDerivado.etiqueta("TEMA_REGISTRADO"));
        assertEquals("", EstadoDerivado.etiqueta(null));
    }
}
