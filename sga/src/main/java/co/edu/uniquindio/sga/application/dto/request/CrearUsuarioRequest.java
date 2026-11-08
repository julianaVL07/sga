package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de entrada con el que el administrador crea un usuario de cualquier rol (Guía 12, 7.2): aquí se asignan los
 * roles internos. Las validaciones son las del registro público (RegistroUsuarioRequest), más el rol.
 * El rol viaja como texto porque RolUsuario es de infraestructura: application no la conoce (3.6).
 */
public record CrearUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo admite máximo 120 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*\\p{Lu})(?=.*\\d).*$",
                message = "La contraseña debe tener al menos una mayúscula y un número")
        String contrasena,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono admite máximo 20 caracteres")
        String telefono,

        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 15, message = "El documento admite máximo 15 caracteres")
        String documento,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser pasada")
        LocalDate fechaNacimiento,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = CambioRolRequest.ROLES,
                message = "El rol debe ser HUESPED, RECEPCIONISTA, ADMINISTRADOR o PERSONAL_SERVICIO")
        String rol
) {
}
