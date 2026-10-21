package co.edu.uniquindio.sga.application.dto.response;

/**
 * Respuesta de CU-11: la reserva modificada y la diferencia de valor asentada en el folio como ajuste (RN-14).
 *
 * @param diferencia en pesos; negativa si la estancia nueva vale menos.
 */
public record ModificacionReservaResponse(
        ReservaDetalleResponse reserva,
        long diferencia
) {
}