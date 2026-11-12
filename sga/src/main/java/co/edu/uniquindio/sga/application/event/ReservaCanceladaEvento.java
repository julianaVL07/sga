package co.edu.uniquindio.sga.application.event;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/**
 * Se publica al cancelar una reserva (CU-09) con la retención de la política congelada (RN-13); el titular se
 * notifica cuando la transacción termina bien (7.8).
 */
public record ReservaCanceladaEvento(Reserva reserva, Dinero retencion) {
}
