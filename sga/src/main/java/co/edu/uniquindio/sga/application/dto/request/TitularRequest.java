package co.edu.uniquindio.sga.application.dto.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Titular de la reserva (3.2). Todos sus datos son obligatorios. El documento viaja como dos textos
 * (tipo y número) porque DocumentoIdentidad es un objeto de valor del dominio.
 */
public record TitularRequest(
    @NotBlank(message = "El nombre del titular es obligatorio")
    String nombre,

    @NotBlank(message = "El tipo de documento del titular es obligatorio")
    String tipoDocumento,

    @NotBlank(message = "El número de documento del titular es obligatorio")
    String numeroDocumento,

    @NotBlank(message = "El email del titular es obligatorio")
    @Email(message = "El email del titular no tiene un formato válido")
    String email,

    @NotBlank(message = "El teléfono del titular es obligatorio")
    String telefono
) {
}
