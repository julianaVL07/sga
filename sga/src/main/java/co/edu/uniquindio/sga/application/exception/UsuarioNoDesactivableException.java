package co.edu.uniquindio.sga.application.exception;

/**
 * El administrador no puede desactivar al usuario (Guía 12, 7.2): es titular de reservas vigentes o es él mismo.
 * Excepción de aplicación: el dominio no conoce al usuario (3.6). El adaptador REST la traduce a 409.
 */
public class UsuarioNoDesactivableException extends RuntimeException {

    private UsuarioNoDesactivableException(String mensaje) {
        super(mensaje);
    }

    public static UsuarioNoDesactivableException porReservasActivas() {
        return new UsuarioNoDesactivableException(
                "No se puede desactivar: el usuario es titular de reservas PENDIENTE, CONFIRMADA o EN_CURSO");
    }

    public static UsuarioNoDesactivableException porSerElMismo() {
        return new UsuarioNoDesactivableException("No se puede desactivar a sí mismo");
    }
}
