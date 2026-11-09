package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

/** Usuario tal como lo ven el administrador y el propio usuario (Guía 12, 7.2): nunca incluye la contraseña */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String documento,
        LocalDate fechaNacimiento,
        String fotoUrl,
        String rol,
        boolean activo
) {
}
