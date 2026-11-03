package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: L-13 (tiempo de preparación entre 0 y 24 horas), implementada en el constructor de
 * TiempoPreparacion.
 */
class TiempoPreparacionTest {

    @Test
    @DisplayName("L-13: el tiempo de preparación de 0, 3 y 24 horas se acepta (los límites están incluidos)")
    void aceptarTiempoEntreCeroYVeinticuatroHoras() {
        for (int horas : new int[] {0, 3, 24}) {
            assertEquals(horas, new TiempoPreparacion(horas).horas());
        }
    }

    @Test
    @DisplayName("L-13: el tiempo de preparación de -1 o 25 horas se rechaza")
    void rechazarTiempoFueraDeRango() {
        for (int horas : new int[] {-1, 25}) {
            ReglaDominioException error = assertThrows(ReglaDominioException.class,
                    () -> new TiempoPreparacion(horas));
            assertTrue(error.getMessage().startsWith("L-13"), String.valueOf(horas));
        }
    }
}
