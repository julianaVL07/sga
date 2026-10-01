package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Rango de fechas de un bloqueo o de una temporada. Usa la misma convención de 3.1:
 * [fechaInicio, fechaFin), es decir, la fecha fin no queda incluida.
 * Ej.: la temporada alta "15 de junio al 20 de julio" se registra como (15-jun, 21-jul).
 *
 * Reglas: RN-07 (un bloqueo vigente sobre una noche la deja no disponible).
 */
public record RangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {

    public RangoFechas {
        if (fechaInicio == null || fechaFin == null) {
            throw new ReglaDominioException("RN-07: el rango debe tener fecha de inicio y fecha de fin");
        }
        if (!fechaFin.isAfter(fechaInicio)) {
            throw new ReglaDominioException("RN-07: la fecha de fin debe ser posterior a la de inicio");
        }
    }

    public int noches() {
        return (int) ChronoUnit.DAYS.between(fechaInicio, fechaFin);
    }

    public boolean seSolapaCon(RangoFechas otro) {
        if (otro == null) {
            throw new ReglaDominioException("RN-07: el rango a comparar es obligatorio");
        }
        return fechaInicio.isBefore(otro.fechaFin) && otro.fechaInicio.isBefore(fechaFin);
    }

    public boolean contiene(LocalDate fecha) {
        return fecha != null && !fecha.isBefore(fechaInicio) && fecha.isBefore(fechaFin);
    }
}
