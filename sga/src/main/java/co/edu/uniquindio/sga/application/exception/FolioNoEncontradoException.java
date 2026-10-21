package co.edu.uniquindio.sga.application.exception;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/**
 * La reserva existe pero no tiene folio. Es una excepción de aplicación, no de dominio: la lanzan los casos de uso
 * de folio (CU-06, CU-07, CU-15) al buscarlo, antes de llamar al dominio. El adaptador REST la traduce a 404.
 */
public class FolioNoEncontradoException extends RuntimeException {

    public FolioNoEncontradoException(CodigoReserva codigo) {
        super("No se encontró el folio de la reserva con código: " + (codigo == null ? "(vacío)" : codigo.valor()));
    }
}