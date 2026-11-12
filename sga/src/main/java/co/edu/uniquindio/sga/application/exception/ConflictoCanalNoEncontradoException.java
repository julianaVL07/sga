package co.edu.uniquindio.sga.application.exception;

/** El conflicto de canal pedido no existe. Excepción de aplicación: el adaptador REST la traduce a 404. */
public class ConflictoCanalNoEncontradoException extends RuntimeException {

    public ConflictoCanalNoEncontradoException(String id) {
        super("No se encontró el conflicto de canal con id: " + (id == null || id.isBlank() ? "(vacío)" : id));
    }
}
