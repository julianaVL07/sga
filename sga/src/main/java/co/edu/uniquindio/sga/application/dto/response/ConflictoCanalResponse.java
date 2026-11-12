package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.EstadoConflictoCanal;

import java.time.LocalDate;

/**
 * Conflicto de canal para el administrador (RN-18): la reserva externa rechazada, con qué colisionó y, si ya se
 * resolvió, el motivo, quién y cuándo.
 */
public record ConflictoCanalResponse(
        String id,
        String canal,
        String identificadorExterno,
        String detalle,
        LocalDate fecha,
        EstadoConflictoCanal estado,
        String motivoResolucion,
        String resueltoPor,
        LocalDate fechaResolucion
) {
}
