package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-21, implementada en VencimientoReservasService.cancelarReservasExpiradas. */
class VencimientoReservasServiceTest {

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final VencimientoReservasService vencimiento =
            new VencimientoReservasService(escenario.reservas, escenario.folios, escenario.alojamientos);
    private final Estancia estancia = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));

    @Test
    @DisplayName("RN-21: una reserva PENDIENTE que superó el plazo de confirmación pasa a CANCELADA")
    void cancelarPendienteQueSuperoElPlazo() {
        // Creada el 1 de octubre a las 9:00; el plazo de 12 horas vence a las 21:00.
        Reserva reserva = escenario.reservaPendiente(1, estancia, LocalTime.of(16, 0));

        int canceladas = vencimiento.cancelarReservasExpiradas(EscenarioCoral.AHORA.plusHours(13));

        assertEquals(1, canceladas);
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertFalse(reserva.ocupaNochesDe(estancia));
    }

    @Test
    @DisplayName("RN-21: una reserva PENDIENTE dentro del plazo de confirmación sigue PENDIENTE")
    void conservarPendienteDentroDelPlazo() {
        Reserva reserva = escenario.reservaPendiente(1, estancia, LocalTime.of(16, 0));

        int canceladas = vencimiento.cancelarReservasExpiradas(EscenarioCoral.AHORA.plusHours(11));

        assertEquals(0, canceladas);
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertTrue(reserva.ocupaNochesDe(estancia));
    }
}
