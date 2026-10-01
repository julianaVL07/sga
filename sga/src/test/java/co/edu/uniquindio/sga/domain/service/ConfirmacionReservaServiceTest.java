package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.MedioPago;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: L-11, implementada en ConfirmacionReservaService.confirmar. */
class ConfirmacionReservaServiceTest {

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final ConfirmacionReservaService confirmacion =
            new ConfirmacionReservaService(escenario.reservas, escenario.folios, escenario.alojamientos);

    @Test
    @DisplayName("L-11: sin el 50 % de anticipo pagado la reserva no se confirma")
    void rechazarConfirmacionSiAnticipoEstaIncompleto() {
        // Dos noches en temporada base: valor $620.000, anticipo exigido $310.000.
        Estancia estancia = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));
        Reserva reserva = escenario.reservaPendiente(1, estancia, LocalTime.of(16, 0));
        Folio folio = Folio.abrir("FOL-" + reserva.getCodigo().valor(), reserva.getCodigo(),
                new Cargo(TipoCargo.ALOJAMIENTO, "Alojamiento", reserva.valorTotal(), LocalDate.of(2026, 10, 1)),
                DepositoGarantia.de("Depósito de garantía", Dinero.de(250_000)), LocalDate.of(2026, 10, 1));
        folio.registrarPago(new Pago(Dinero.de(200_000), MedioPago.TRANSFERENCIA_BANCARIA, LocalDate.of(2026, 10, 1)));
        escenario.folios.guardar(folio);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> confirmacion.confirmar(reserva.getCodigo(), EscenarioCoral.AHORA.plusHours(1)));

        assertTrue(error.getMessage().startsWith("L-11"));
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }
}
