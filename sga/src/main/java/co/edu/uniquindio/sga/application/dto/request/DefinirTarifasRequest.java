package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * CU-24. PUT /api/apartamentos/{id}/tarifas
 * Una o varias tarifas por temporada que rigen desde hoy; las anteriores quedan en el histórico (7.4).
 */
public record DefinirTarifasRequest(
        @NotEmpty(message = "Debe indicar al menos una tarifa")
        List<@Valid @NotNull(message = "La tarifa no puede ser nula") TarifaRequest> tarifas
) {
}
