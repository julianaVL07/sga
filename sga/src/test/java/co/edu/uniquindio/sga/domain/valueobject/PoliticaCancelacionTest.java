package co.edu.uniquindio.sga.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Matriz de trazabilidad: RN-13, implementada en PoliticaCancelacion.calcularRetencion().
 * Tramos del Anexo A: 16 días o más → 0 %, de 7 a 15 → 25 %, menos de 7 o no-show → 50 %.
 */
class PoliticaCancelacionTest {

    private final PoliticaCancelacion politica = new PoliticaCancelacion(1, LocalDate.of(2026, 1, 1),
            "Retención del 50 % del total", List.of(new TramoCancelacion(16, 0), new TramoCancelacion(7, 25),
            new TramoCancelacion(0, 50)));

    @Test
    @DisplayName("RN-13: la retención depende del tramo de antelación en que se cancela")
    void calcularRetencionSegunTramo() {
        Dinero total = Dinero.de(1_240_000);
        LocalDate entrada = LocalDate.of(2026, 11, 20);

        assertEquals(Dinero.cero(), politica.calcularRetencion(total, entrada, entrada.minusDays(16)));
        assertEquals(Dinero.de(310_000), politica.calcularRetencion(total, entrada, entrada.minusDays(15)));
        assertEquals(Dinero.de(310_000), politica.calcularRetencion(total, entrada, entrada.minusDays(7)));
        assertEquals(Dinero.de(620_000), politica.calcularRetencion(total, entrada, entrada.minusDays(6)));
        assertEquals(Dinero.de(620_000), politica.calcularRetencionNoShow(total));
    }
}
