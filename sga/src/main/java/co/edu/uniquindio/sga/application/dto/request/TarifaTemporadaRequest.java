package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * CU-29. Tarifa de un apartamento en la temporada que se crea: valor por ocupante facturable y por noche, en pesos
 * enteros (7.4). Que el apartamento exista y que el valor sea mayor que cero son reglas del dominio (409).
 */
public record TarifaTemporadaRequest(
        @NotBlank(message = "El apartamento de la tarifa es obligatorio")
        String identificacionApartamento,

        @NotNull(message = "El valor por ocupante es obligatorio")
        Long valorPorOcupante
) {
}
