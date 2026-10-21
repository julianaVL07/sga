package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * CU-08. POST /api/canales/{canal}/reservas
 * Reserva enviada por el canal externo. El identificador externo es obligatorio: con él se detecta un mensaje
 * repetido y no se crea otra reserva (RN-19). Si las noches ya no están libres, el dominio rechaza el mensaje y
 * registra un conflicto (RN-18). La reserva nace PENDIENTE con su folio, igual que en CU-03.
 */
public record ReservaExternaRequest(
        @NotBlank(message = "El identificador de la reserva en el canal es obligatorio")
        String identificadorExterno,

        @NotBlank(message = "La identificación del apartamento es obligatoria")
        String identificacionApartamento,

        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,

        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,

        LocalTime horaEstimadaLlegada,           // el canal puede no enviarla; se exige al confirmar (RN-09)

        @NotNull(message = "La reserva debe tener titular")
        @Valid
        TitularRequest titular,

        @NotEmpty(message = "La reserva debe tener al menos un ocupante")
        List<@Valid OcupanteRequest> ocupantes
) {
}
