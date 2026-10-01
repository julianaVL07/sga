package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * Abono registrado contra un folio, con medio y fecha (glosario, F-08).
 * Reglas: RN-15 (todo pago tiene medio y fecha), RN-16 (no se modifica ni se elimina).
 */
public record Pago(Dinero valor, MedioPago medio, LocalDate fecha) {

    public Pago {
        if (valor == null || !valor.esPositivo()) {
            throw new ReglaDominioException("RN-15: el valor del pago debe ser mayor que cero");
        }
        if (medio == null) {
            throw new ReglaDominioException("RN-15: todo pago debe indicar su medio de pago");
        }
        if (fecha == null) {
            throw new ReglaDominioException("RN-15: todo pago debe indicar su fecha");
        }
    }
}
