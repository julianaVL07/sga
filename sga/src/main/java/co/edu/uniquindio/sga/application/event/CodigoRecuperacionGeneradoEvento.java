package co.edu.uniquindio.sga.application.event;

/**
 * Se publica al generar un código para restablecer la contraseña (7.2). Lleva el código en claro solo en memoria,
 * para enviarlo; en la base de datos se guarda cifrado.
 */
public record CodigoRecuperacionGeneradoEvento(String correo, String nombre, String codigo, long minutosVigencia) {

    /** Que el código no aparezca si el evento se escribe en el log. */
    @Override
    public String toString() {
        return "CodigoRecuperacionGeneradoEvento[correo=" + correo + ", minutosVigencia=" + minutosVigencia + "]";
    }
}
