package co.edu.uniquindio.sga.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-06, implementada en Ocupante.esFacturableEn(). Umbral del Anexo A: 6 años. */
class OcupanteTest {

    private static final UmbralEdadFacturable UMBRAL = new UmbralEdadFacturable(6);

    @Test
    @DisplayName("RN-06: es facturable quien a la fecha de entrada ya cumplió el umbral, aunque sea ese mismo día")
    void marcarFacturableSiAlcanzaUmbralAFechaEntrada() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        Ocupante cumpleSeisElDiaDeEntrada = new Ocupante("Sofía", null, LocalDate.of(2020, 10, 10));
        Ocupante cumpleSeisDuranteLaEstancia = new Ocupante("Tomás", null, LocalDate.of(2020, 10, 11));

        assertTrue(cumpleSeisElDiaDeEntrada.esFacturableEn(estancia, UMBRAL));
        // 3.2: quien cumple años durante la estancia no cambia de condición.
        assertFalse(cumpleSeisDuranteLaEstancia.esFacturableEn(estancia, UMBRAL));
    }
}
