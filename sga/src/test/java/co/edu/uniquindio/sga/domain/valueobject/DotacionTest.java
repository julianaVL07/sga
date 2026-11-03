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
 * Matriz de trazabilidad: L-06 (la dotación tiene al menos un elemento y ninguno en blanco), implementada en el
 * constructor de Dotacion.
 */
class DotacionTest {

    @Test
    @DisplayName("L-06: una dotación con elementos se acepta, sin espacios sobrantes ni repetidos")
    void aceptarDotacionConElementos() {
        Dotacion dotacion = new Dotacion(List.of(" Aire acondicionado ", "Cocina", "Cocina"));

        assertEquals(List.of("Aire acondicionado", "Cocina"), dotacion.elementos());
        assertTrue(dotacion.contieneElemento("cocina"));
    }

    @Test
    @DisplayName("L-06: una dotación vacía, nula o con elementos en blanco se rechaza")
    void rechazarDotacionVaciaOConElementosEnBlanco() {
        List<List<String>> invalidas = Arrays.asList(null, List.of(), List.of("Cocina", "  "),
                Arrays.asList("Cocina", null));
        for (List<String> elementos : invalidas) {
            ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> new Dotacion(elementos));
            assertTrue(error.getMessage().startsWith("L-06"), String.valueOf(elementos));
        }
    }
}
