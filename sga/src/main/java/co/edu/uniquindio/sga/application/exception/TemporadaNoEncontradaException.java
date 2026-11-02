package co.edu.uniquindio.sga.application.exception;

/**
 * La temporada pedida no existe en el alojamiento. Es una excepción de aplicación, no de dominio: "no existe" no es
 * una regla de negocio. El adaptador REST la traduce a 404.
 */
public class TemporadaNoEncontradaException extends RuntimeException {

    public TemporadaNoEncontradaException(String idTemporada) {
        super("No se encontró la temporada con id: " + (idTemporada == null ? "(vacío)" : idTemporada));
    }
}
