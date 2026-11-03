package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: formato del código de reserva RES-YYYY-NNNNN, implementado en el constructor de
 * CodigoReserva y en CodigoReserva.generar (secuencia entre 1 y 99999).
 */
class CodigoReservaTest {

    @Test
    @DisplayName("RES-YYYY-NNNNN: un código con el formato se acepta, sin espacios y en mayúsculas")
    void aceptarFormatoValido() {
        CodigoReserva codigo = new CodigoReserva(" res-2026-00042 ");

        assertEquals("RES-2026-00042", codigo.valor());
        assertEquals(2026, codigo.anio());
        assertEquals(42, codigo.secuencia());
        assertEquals("RES-2026-00001", CodigoReserva.generar(2026, 1).valor());
        assertEquals("RES-2026-99999", CodigoReserva.generar(2026, 99_999).valor());
    }

    @Test
    @DisplayName("RES-YYYY-NNNNN: un código vacío o con otro formato se rechaza")
    void rechazarFormatoInvalido() {
        List<String> invalidos = Arrays.asList(null, "  ", "RES-2026-0001", "RES-26-00001", "RSV-2026-00001",
                "RES-2026-000001", "RES-2026-ABCDE");
        for (String valor : invalidos) {
            ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> new CodigoReserva(valor));
            assertTrue(error.getMessage().startsWith("El código de la reserva"), String.valueOf(valor));
        }
    }

    @Test
    @DisplayName("RES-YYYY-NNNNN: generar con una secuencia de 0 o 100000 se rechaza")
    void rechazarSecuenciaFueraDeRango() {
        for (int secuencia : new int[] {0, 100_000}) {
            ReglaDominioException error = assertThrows(ReglaDominioException.class,
                    () -> CodigoReserva.generar(2026, secuencia));
            assertTrue(error.getMessage().startsWith("La secuencia"), String.valueOf(secuencia));
        }
    }
}
