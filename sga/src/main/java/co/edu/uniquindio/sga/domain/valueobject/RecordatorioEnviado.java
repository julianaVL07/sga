package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Último recordatorio que se le envió al titular de una reserva por un motivo (7.8).
 *
 * @param fechaEvento la fecha que se recordó: la de entrada (LLEGADA) o la de salida (SALIDA). Si la reserva se
 *                    modifica y la fecha cambia, el recordatorio de la fecha nueva sí se envía.
 * @param enviadoEn   cuándo se envió, en la hora de Colombia.
 */
public record RecordatorioEnviado(CodigoReserva codigo, MotivoRecordatorio motivo, LocalDate fechaEvento,
                                  LocalDateTime enviadoEn) {

    public RecordatorioEnviado {
        if (codigo == null || motivo == null || fechaEvento == null || enviadoEn == null) {
            throw new ReglaDominioException("7.8: el recordatorio necesita la reserva, el motivo, la fecha y el envío");
        }
    }

    /** Un mismo recordatorio no se envía dos veces: ya se avisó de esa fecha por ese motivo. */
    public boolean cubre(LocalDate fecha) {
        return fechaEvento.equals(fecha);
    }
}
