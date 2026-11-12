package co.edu.uniquindio.sga.application.exception;

/** El canal de venta pedido no existe. Excepción de aplicación: el adaptador REST la traduce a 404. */
public class CanalVentaNoEncontradoException extends RuntimeException {

    public CanalVentaNoEncontradoException(String nombre) {
        super("No se encontró el canal de venta: " + (nombre == null || nombre.isBlank() ? "(vacío)" : nombre));
    }
}
