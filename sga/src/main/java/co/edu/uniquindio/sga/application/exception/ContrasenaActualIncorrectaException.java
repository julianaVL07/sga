package co.edu.uniquindio.sga.application.exception;

/**
 * La contraseña actual del cambio de contraseña no coincide (Guía 12, 7.2). Es un dato de entrada errado del propio
 * usuario, ya autenticado con su token: el adaptador REST la traduce a 400, no a 401.
 */
public class ContrasenaActualIncorrectaException extends RuntimeException {

    public ContrasenaActualIncorrectaException() {
        super("La contraseña actual no es correcta");
    }
}
