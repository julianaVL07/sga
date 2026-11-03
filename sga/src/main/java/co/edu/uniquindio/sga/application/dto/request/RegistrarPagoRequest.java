package co.edu.uniquindio.sga.application.dto.request;

import co.edu.uniquindio.sga.domain.valueobject.MedioPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * CU-15. POST /api/reservas/{codigo}/folio/pagos
 * Abono contra el folio con su medio (RN-15). La fecha la pone el SGA con el reloj de Colombia. Un pago no se edita
 * ni se borra: una corrección es un movimiento inverso (RN-16).
 */
public record RegistrarPagoRequest(
        @NotNull(message = "El valor del pago es obligatorio")
        @Positive(message = "El valor del pago debe ser mayor que cero")
        Long valor,                              // pesos enteros, nunca double

        @NotNull(message = "El medio de pago es obligatorio")
        MedioPago medio                          // TARJETA_DEBITO_CREDITO o TRANSFERENCIA_BANCARIA (L-18)
) {
}