package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * Concepto que suma al folio: alojamiento, servicio, penalidad o ajuste (glosario, 7.7).
 * Es inmutable: nunca se modifica ni se elimina (RN-16). Puede ser negativo cuando es un ajuste
 * a la baja (RN-14) o un movimiento inverso (RN-16).
 */
public record Cargo(TipoCargo tipo, String concepto, Dinero valor, LocalDate fecha) {

    public Cargo {
        if (tipo == null) {
            throw new ReglaDominioException("RN-15: el cargo debe tener tipo");
        }
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaDominioException("RN-15: el cargo debe tener concepto");
        }
        if (valor == null || valor.esCero()) {
            throw new ReglaDominioException("RN-15: el cargo debe tener un valor distinto de cero");
        }
        if (fecha == null) {
            throw new ReglaDominioException("RN-15: el cargo debe tener fecha");
        }
        concepto = concepto.trim();
    }

    public boolean esDeAlojamiento() {
        return tipo == TipoCargo.ALOJAMIENTO;
    }

    /** RN-16: la corrección de un cargo es otro cargo del mismo tipo con el valor opuesto. */
    public Cargo inverso(String motivo, LocalDate fechaCorreccion) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("RN-16: la corrección debe indicar un motivo");
        }
        return new Cargo(tipo, "Reverso de '" + concepto + "': " + motivo.trim(), valor.negar(), fechaCorreccion);
    }
}
