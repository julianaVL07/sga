package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * La novedad pedida no existe o no es del apartamento de la URL. Es una excepción de aplicación, no de dominio:
 * "no existe" no es una regla de negocio. El adaptador REST la traduce a 404.
 */
public class NovedadNoEncontradaException extends RuntimeException {

    public NovedadNoEncontradaException(IdentificacionApartamento apartamento, String idNovedad) {
        super("No se encontró la novedad " + (idNovedad == null ? "(vacío)" : idNovedad)
                + " del apartamento " + (apartamento == null ? "(vacío)" : apartamento.codigo()));
    }
}
