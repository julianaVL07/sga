package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.EstadoNoche;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

import java.time.LocalDate;

/**
 * Una noche del calendario de ocupación (CU-27, 7.1). codigoReserva y estadoReserva solo vienen si la noche está
 * RESERVADA; motivoBloqueo solo si está BLOQUEADA. Los demás campos van en null.
 */
public record NocheCalendarioResponse(
        LocalDate fecha,
        EstadoNoche estado,
        String codigoReserva,                 // "RES-2026-00003"
        EstadoReserva estadoReserva,
        String motivoBloqueo
) {
}
