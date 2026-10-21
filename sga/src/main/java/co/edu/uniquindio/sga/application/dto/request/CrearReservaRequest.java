package co.edu.uniquindio.sga.application.dto.request;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * CU-03. POST /api/reservas
 * Solo trae lo que decide quien reserva. No trae lo que genera el SGA: el código RES-YYYY-NNNNN, el estado
 * (siempre nace PENDIENTE), la fecha de creación ni la cotización (la calcula CotizadorEstanciaService).
 * Las reglas de fechas (RN-03, RN-04), capacidad (RN-02) y solapamiento (RN-01) las valida el dominio (409),
 * no este DTO: aquí solo se revisa la forma del mensaje (400).
 */
public record CrearReservaRequest(
        @NotBlank(message = "La identificación del apartamento es obligatoria")
        String identificacionApartamento,           // "APT-101": texto, no objeto de valor

        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,

        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,

        LocalTime horaEstimadaLlegada,              // opcional al crear, obligatoria para confirmar (RN-09); desde las 20:00 genera RP-03

        @NotNull(message = "El canal de origen es obligatorio")
        CanalOrigen canalOrigen,                    // PORTAL o DIRECTO; EXTERNO entra por CU-08 (RN-19)

        @NotNull(message = "La reserva debe tener titular")
        @Valid
        TitularRequest titular,

        @NotEmpty(message = "La reserva debe tener al menos un ocupante")
        List<@Valid OcupanteRequest> ocupantes
) {
}
