package co.edu.uniquindio.sga.application.event;

import co.edu.uniquindio.sga.domain.entity.Reserva;

/** Se publica al confirmar una reserva (CU-10); el titular se notifica cuando la transacción termina bien (7.8). */
public record ReservaConfirmadaEvento(Reserva reserva) {
}
