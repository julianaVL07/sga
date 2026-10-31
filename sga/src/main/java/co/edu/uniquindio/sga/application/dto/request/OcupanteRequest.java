package co.edu.uniquindio.sga.application.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * Ocupante de la reserva (3.2, 7.2): el nombre y la fecha de nacimiento son obligatorios; el documento es opcional.
 * La edad nunca viaja ni se guarda: el dominio la calcula con la fecha de nacimiento (RN-06, umbral de 6 años en Coral).
 */
public record OcupanteRequest(
        @NotBlank(message = "El nombre del ocupante es obligatorio")
        String nombre,

        String tipoDocumento,      // opcional, junto con numeroDocumento
        String numeroDocumento,    // opcional

        @NotNull(message = "La fecha de nacimiento del ocupante es obligatoria")
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
        LocalDate fechaNacimiento
) {
}
