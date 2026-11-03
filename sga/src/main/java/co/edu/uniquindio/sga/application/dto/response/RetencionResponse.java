package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/**
 * Respuesta de CU-09 (cancelar) y CU-12 (no-show): la retención asentada en el folio según la política
 * congelada (RN-13). Lo pagado por encima de la retención queda como saldo a favor del titular.
 *
 * @param estado    CANCELADA o NO_SHOW.
 * @param retencion en pesos.
 */
public record RetencionResponse(
        String codigo,
        EstadoReserva estado,
        long retencion
) {
}