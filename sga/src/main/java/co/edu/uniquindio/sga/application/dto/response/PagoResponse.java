package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.MedioPago;

import java.time.LocalDate;

/** Un pago registrado en el folio con su medio y su fecha (RN-15). El valor va en pesos enteros. */
public record PagoResponse(
        long valor,
        MedioPago medio,
        LocalDate fecha
) {
}