package co.edu.uniquindio.sga.application.exception;

/** El usuario pedido no existe (Guía 12, 7.2). Excepción de aplicación: el dominio no conoce al usuario (3.6). 404 */
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(Long id) {
        super("No existe un usuario con id " + id);
    }

    public UsuarioNoEncontradoException(String correo) {
        super("No existe un usuario con el correo " + correo);
    }
}
