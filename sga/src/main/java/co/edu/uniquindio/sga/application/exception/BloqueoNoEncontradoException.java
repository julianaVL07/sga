package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * El bloqueo pedido no existe o no es del apartamento de la URL. Es una excepción de aplicación, no de dominio:
 * "no existe" no es una regla de negocio. El adaptador REST la traduce a 404.
 */
public class BloqueoNoEncontradoException extends RuntimeException {

    public BloqueoNoEncontradoException(IdentificacionApartamento apartamento, String idBloqueo) {
        super("No se encontró el bloqueo " + (idBloqueo == null ? "(vacío)" : idBloqueo)
                + " del apartamento " + (apartamento == null ? "(vacío)" : apartamento.codigo()));
    }
}
