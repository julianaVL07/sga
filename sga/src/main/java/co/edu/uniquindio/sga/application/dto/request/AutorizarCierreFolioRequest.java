package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * CU-07. POST /api/reservas/{codigo}/folio/autorizacion-cierre
 * El administrador autoriza cerrar el folio con saldo distinto de cero; queda registrado quién y por qué (RN-17).
 */
public record AutorizarCierreFolioRequest(
        @NotBlank(message = "Debe indicar quién autoriza el cierre")
        String autorizadoPor,

        @NotBlank(message = "El motivo de la autorización es obligatorio")
        String motivo
) {
}