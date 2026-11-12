package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada de POST /api/auth/recuperacion/restablecer (7.2).
 * La contraseña nueva sigue la misma regla del registro.
 */
public record RestablecerContrasenaRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo admite máximo 120 caracteres")
        String correo,

        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos")
        String codigo,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*\\p{Lu})(?=.*\\d).*$",
                message = "La contraseña debe tener al menos una mayúscula y un número")
        String nuevaContrasena
) {
}
