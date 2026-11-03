package co.edu.uniquindio.sga.application.dto.request;
import java.time.LocalTime;

/**
 * CU-10. PUT /api/reservas/{codigo}/confirmar
 * La hora estimada de llegada es opcional si ya se registró al crear; si la reserva no la tiene, el dominio
 * rechaza la confirmación (RN-09). El anticipo del 50 % (L-11) y el plazo (RN-21) también los valida el dominio.
 */
public record ConfirmarReservaRequest(
        LocalTime horaEstimadaLlegada
) {
}
