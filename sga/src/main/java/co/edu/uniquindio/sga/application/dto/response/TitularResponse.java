package co.edu.uniquindio.sga.application.dto.response;

/** Titular de la reserva aplanado a textos. */
public record TitularResponse(
        String nombre,
        String tipoDocumento,
        String numeroDocumento,
        String email,
        String telefono
) {
}
