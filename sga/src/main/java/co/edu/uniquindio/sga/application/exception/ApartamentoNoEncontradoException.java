package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * El apartamento pedido no existe. Es una excepción de aplicación, no de dominio: la lanzan los casos de uso
 * al buscar el apartamento, antes de llamar al dominio. "No existe" no es una regla de negocio, así que no usa
 * ReglaDominioException. El adaptador REST la traduce a 404.
 */
public class ApartamentoNoEncontradoException extends RuntimeException {

    public ApartamentoNoEncontradoException(IdentificacionApartamento identificacion) {
        super("No se encontró el apartamento con identificación: "
                + (identificacion == null ? "(vacía)" : identificacion.codigo()));
    }
}
