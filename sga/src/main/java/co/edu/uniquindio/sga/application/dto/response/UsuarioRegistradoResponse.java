package co.edu.uniquindio.sga.application.dto.response;

/** DTO de salida del registro público (Guía 12): nunca incluye la contraseña */
public record UsuarioRegistradoResponse(
        Long id,
        String nombre,
        String correo,
        String rol
) {
}
