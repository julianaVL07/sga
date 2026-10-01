package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Rango continuo de noches en que un apartamento queda ocupado por una reserva (glosario, 3.1).
 * Intervalo cerrado en la entrada y abierto en la salida: [entrada, salida).
 *
 * Reglas: RN-03 (la salida es posterior a la entrada: toda estancia tiene al menos una noche),
 * RN-01 (definición de solapamiento de 3.1).
 */
public record Estancia(LocalDate fechaEntrada, LocalDate fechaSalida) {

    public Estancia {
        if (fechaEntrada == null || fechaSalida == null) {
            throw new ReglaDominioException("RN-03: la estancia debe tener fecha de entrada y fecha de salida");
        }
        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new ReglaDominioException("RN-03: la fecha de salida debe ser posterior a la de entrada");
        }
    }

    /** 3.1: número de noches = días calendario entre entrada y salida (del 10 al 12 son dos noches). */
    public int noches() {
        return (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }

    /** 3.1: se solapan si entrada_A &lt; salida_B y entrada_B &lt; salida_A. */
    public boolean seSolapaCon(Estancia otra) {
        if (otra == null) {
            throw new ReglaDominioException("RN-01: la estancia a comparar es obligatoria");
        }
        return fechaEntrada.isBefore(otra.fechaSalida) && otra.fechaEntrada.isBefore(fechaSalida);
    }

    /** Indica si la noche de esa fecha pertenece a la estancia. La fecha de salida no se ocupa. */
    public boolean incluyeFecha(LocalDate fecha) {
        return fecha != null && !fecha.isBefore(fechaEntrada) && fecha.isBefore(fechaSalida);
    }

    /** Las noches de la estancia, en orden, para liquidar noche por noche (RN-05). */
    public List<LocalDate> fechasDeNoches() {
        return fechaEntrada.datesUntil(fechaSalida).toList();
    }
}
