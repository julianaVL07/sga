package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * CU-30. Un tramo de la política de cancelación (L-10): desde cuántos días de antelación se retiene qué porcentaje
 * del valor total. Que la antelación no sea negativa y el porcentaje esté entre 0 y 100 lo valida el dominio (409).
 */
public record TramoCancelacionRequest(
        @NotNull(message = "La antelación mínima en días es obligatoria")
        Integer antelacionMinimaDias,

        @NotNull(message = "El porcentaje de retención es obligatorio")
        Integer porcentajeRetencion
) {
}
