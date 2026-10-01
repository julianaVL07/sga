package co.edu.uniquindio.sga.domain.exception;
/**

 * Se lanza cuando una operación viola una regla del negocio.

 * No representa fallas técnicas.

 */

public class ReglaDominioException extends RuntimeException {

    public ReglaDominioException(String message) {
        super(message);
    }
}
