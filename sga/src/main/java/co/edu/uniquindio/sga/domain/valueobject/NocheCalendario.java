package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * Una noche del calendario de ocupación (CU-27, 7.1). La noche de una fecha va de esa fecha a la siguiente (3.1).
 * Si está RESERVADA lleva el código y el estado de la reserva; si está BLOQUEADA, el motivo del bloqueo.
 * Los demás campos van en null.
 */
public record NocheCalendario(LocalDate fecha, EstadoNoche estado, CodigoReserva codigoReserva,
                              EstadoReserva estadoReserva, String motivoBloqueo) {

    public NocheCalendario {
        if (fecha == null || estado == null) {
            throw new ReglaDominioException("7.1: la noche del calendario debe tener fecha y estado");
        }
    }

    public static NocheCalendario libre(LocalDate fecha) {
        return new NocheCalendario(fecha, EstadoNoche.LIBRE, null, null, null);
    }

    public static NocheCalendario reservada(LocalDate fecha, Reserva reserva) {
        return new NocheCalendario(fecha, EstadoNoche.RESERVADA, reserva.getCodigo(), reserva.getEstado(), null);
    }

    public static NocheCalendario bloqueada(LocalDate fecha, Bloqueo bloqueo) {
        return new NocheCalendario(fecha, EstadoNoche.BLOQUEADA, null, null, bloqueo.getMotivo());
    }
}
