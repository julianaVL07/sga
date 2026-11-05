package co.edu.uniquindio.sga.application.exception;

/**
 * El correo o el documento del registro ya pertenecen a otro usuario. Es una excepción de aplicación, no de dominio:
 * el dominio no conoce al usuario (3.6). El adaptador REST la traduce a 409.
 */
public class UsuarioYaRegistradoException extends RuntimeException {

    public UsuarioYaRegistradoException() {
        super("Ya existe un usuario registrado con ese correo o documento");
    }
}
