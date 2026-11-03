package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * CU-22 y CU-24. Valor por ocupante facturable y por noche en una temporada (7.4), en pesos enteros.
 * Que la temporada exista y que el valor sea mayor que cero son reglas del dominio (409), no de este DTO.
 */
public record TarifaRequest(
        @NotBlank(message = "La temporada de la tarifa es obligatoria")
        String idTemporada,

        @NotNull(message = "El valor por ocupante es obligatorio")
        Long valorPorOcupante
) {
}
