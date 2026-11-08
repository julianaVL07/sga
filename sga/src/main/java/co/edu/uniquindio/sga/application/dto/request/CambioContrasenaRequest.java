package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO de entrada de PUT /api/usuarios/me/contrasena (Guía 12, 7.2). La nueva sigue la regla del registro */
public record CambioContrasenaRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String contrasenaActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*\\p{Lu})(?=.*\\d).*$",
                message = "La contraseña debe tener al menos una mayúscula y un número")
        String contrasenaNueva
) {
}
