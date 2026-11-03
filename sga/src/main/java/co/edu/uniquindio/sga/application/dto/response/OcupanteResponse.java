package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

/** Ocupante de la reserva. El documento llega en null si no se registró (7.2). La edad no se expone ni se guarda. */
public record OcupanteResponse(
        String nombre,
        String tipoDocumento,
        String numeroDocumento,
        LocalDate fechaNacimiento
) {
}
