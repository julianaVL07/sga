package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/**
 * La reserva pedida no existe. Es una excepción de aplicación, no de dominio: la lanzan los casos de uso
 * al buscar la reserva, antes de llamar a los servicios de dominio. El adaptador REST la traduce a 404.
 */
public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException(CodigoReserva codigo) {
        super("No se encontró la reserva con código: " + (codigo == null ? "(vacío)" : codigo.valor()));
    }
}
