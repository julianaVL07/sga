package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: L-09 (umbral de edad facturable entre 0 y 120 años), implementada en el constructor de
 * UmbralEdadFacturable.
 */
class UmbralEdadFacturableTest {

    @Test
    @DisplayName("L-09: el umbral de 0, 6 y 120 años se acepta (los límites están incluidos)")
    void aceptarUmbralEntreCeroYCientoVeinte() {
        for (int anios : new int[] {0, 6, 120}) {
            assertEquals(anios, new UmbralEdadFacturable(anios).anios());
        }
    }

    @Test
    @DisplayName("L-09: el umbral de -1 o 121 años se rechaza")
    void rechazarUmbralFueraDeRango() {
        for (int anios : new int[] {-1, 121}) {
            ReglaDominioException error = assertThrows(ReglaDominioException.class,
                    () -> new UmbralEdadFacturable(anios));
            assertTrue(error.getMessage().startsWith("L-09"), String.valueOf(anios));
        }
    }
}
