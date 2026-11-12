package co.edu.uniquindio.sga.application.exception;

/**
 * El código para restablecer la contraseña no sirve (7.2): no coincide, venció, ya se usó, se agotaron los intentos
 * o el correo no tiene una cuenta activa. El mensaje es el mismo en todos los casos para no revelar cuentas.
 */
public class CodigoRecuperacionInvalidoException extends RuntimeException {

    public CodigoRecuperacionInvalidoException() {
        super("El código no es válido o ya venció. Solicita uno nuevo.");
    }
}
