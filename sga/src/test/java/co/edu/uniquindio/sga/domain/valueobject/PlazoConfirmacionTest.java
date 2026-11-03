package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: L-14 (plazo de confirmación de al menos una hora), implementada en el constructor de
 * PlazoConfirmacion.
 */
class PlazoConfirmacionTest {

    @Test
    @DisplayName("L-14: el plazo de confirmación de 1 y 12 horas se acepta (una hora es el mínimo)")
    void aceptarPlazoDeAlMenosUnaHora() {
        for (int horas : new int[] {1, 12}) {
            assertEquals(horas, new PlazoConfirmacion(horas).horas());
        }
    }

    @Test
    @DisplayName("L-14: el plazo de confirmación de 0 o -1 horas se rechaza")
    void rechazarPlazoMenorAUnaHora() {
        for (int horas : new int[] {0, -1}) {
            ReglaDominioException error = assertThrows(ReglaDominioException.class,
                    () -> new PlazoConfirmacion(horas));
            assertTrue(error.getMessage().startsWith("L-14"), String.valueOf(horas));
        }
    }
}
