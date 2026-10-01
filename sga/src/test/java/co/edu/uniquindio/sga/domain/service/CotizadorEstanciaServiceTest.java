package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.DetalleNoche;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Matriz de trazabilidad: RN-05, implementada en CotizadorEstanciaService.cotizar. */
class CotizadorEstanciaServiceTest {

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final CotizadorEstanciaService cotizador =
            new CotizadorEstanciaService(escenario.apartamentos, escenario.alojamientos);

    @Test
    @DisplayName("RN-05: cada noche se liquida con la tarifa de su temporada por los ocupantes facturables")
    void liquidarEstanciaNochePorNocheSegunTemporada() {
        // Noche del 4 en temporada base y noche del 5 en temporada alta; el bebé no es facturable (RN-06).
        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 6));
        Ocupante bebe = new Ocupante("Martín", null, LocalDate.of(2024, 3, 1));

        Cotizacion cotizacion = cotizador.cotizar(EscenarioCoral.APT_101, estancia,
                List.of(escenario.ana, bebe), null);

        DetalleNoche nocheBase = cotizacion.detalles().get(0);
        DetalleNoche nocheAlta = cotizacion.detalles().get(1);
        assertEquals(2, cotizacion.totalNoches());
        assertEquals("Base", nocheBase.temporada());
        assertEquals(1, nocheBase.ocupantesFacturables());
        assertEquals(Dinero.de(310_000), nocheBase.subtotal());
        assertEquals("Alta", nocheAlta.temporada());
        assertEquals(Dinero.de(620_000), nocheAlta.subtotal());
        assertEquals(Dinero.de(930_000), cotizacion.valorTotal());
    }
}
