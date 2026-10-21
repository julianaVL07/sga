// src/main/java/co/edu/uniquindio/sga/application/dto/request/CotizarEstanciaRequest.java
package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * CU-02. POST /api/cotizaciones
 * Pide el valor de una estancia sin crear la reserva. Los ocupantes llevan fecha de nacimiento porque el dominio
 * decide quién es facturable con el umbral de edad del Apartahotel Coral (RN-05, 6 años) y calcula noche a noche
 * con la tarifa de su temporada (RN-06, RP-01). Las fechas y la estancia mínima las valida el dominio (409).
 */
public record CotizarEstanciaRequest(
        @NotBlank(message = "La identificación del apartamento es obligatoria")
        String identificacionApartamento,

        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,

        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,

        @NotEmpty(message = "La cotización debe tener al menos un ocupante")
        List<@Valid OcupanteRequest> ocupantes
) {
}