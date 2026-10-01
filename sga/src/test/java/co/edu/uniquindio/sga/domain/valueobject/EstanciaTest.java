package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-03, implementada en el constructor compacto de Estancia. */
class EstanciaTest {

    @Test
    @DisplayName("RN-03: una estancia de entrada 10 y salida 11 es válida y tiene una noche")
    void crearEstanciaValidaConMinimoUnaNoche() {
        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11));

        assertEquals(1, estancia.noches());
        assertEquals(LocalDate.of(2026, 10, 10), estancia.fechaEntrada());
    }

    @Test
    @DisplayName("RN-03: se rechaza la estancia con salida igual o anterior a la entrada")
    void rechazarEstanciaConSalidaIgualOAnteriorAEntrada() {
        LocalDate entrada = LocalDate.of(2026, 10, 10);

        ReglaDominioException igual = assertThrows(ReglaDominioException.class,
                () -> new Estancia(entrada, entrada));
        ReglaDominioException anterior = assertThrows(ReglaDominioException.class,
                () -> new Estancia(entrada, entrada.minusDays(1)));

        assertTrue(igual.getMessage().startsWith("RN-03"));
        assertTrue(anterior.getMessage().startsWith("RN-03"));
    }
}
