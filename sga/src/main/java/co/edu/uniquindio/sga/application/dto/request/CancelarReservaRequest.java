package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * CU-09. PUT /api/reservas/{codigo}/cancelar
 * La retención la calcula el dominio con la política congelada en la reserva (RN-13, RN-22).
 */
public record CancelarReservaRequest(
        @NotBlank(message = "El motivo de la cancelación es obligatorio")
        String motivo
) {
}
