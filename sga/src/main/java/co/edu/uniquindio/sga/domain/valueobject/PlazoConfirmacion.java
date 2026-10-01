package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDateTime;

/**
 * Horas que una reserva PENDIENTE puede esperar confirmación (L-14). En el Anexo A son 12 horas.
 *
 * Reglas: RN-21 (una reserva PENDIENTE que supera el plazo se cancela automáticamente).
 */
public record PlazoConfirmacion(int horas) {

    public PlazoConfirmacion {
        if (horas < 1) {
            throw new ReglaDominioException("L-14: el plazo de confirmación debe ser de al menos una hora");
        }
    }

    /** Vencida cuando la hora actual supera la hora de creación más el plazo. */
    public boolean estaVencida(LocalDateTime fechaCreacion, LocalDateTime fechaActual) {
        if (fechaCreacion == null || fechaActual == null) {
            throw new ReglaDominioException("RN-21: las fechas de creación y actual son obligatorias");
        }
        return fechaActual.isAfter(fechaCreacion.plusHours(horas));
    }
}
