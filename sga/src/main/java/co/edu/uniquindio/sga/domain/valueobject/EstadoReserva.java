package co.edu.uniquindio.sga.domain.valueobject;

/**
 * Estados del ciclo de vida de la reserva (sección 8). Las transiciones válidas (RN-08) las valida Reserva.
 */
public enum EstadoReserva {
    PENDIENTE,
    CONFIRMADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA,
    NO_SHOW;

    /** Sección 8 y RN-12: solo las reservas activas retienen disponibilidad. */
    public boolean retieneDisponibilidad() {
        return this == PENDIENTE || this == CONFIRMADA || this == EN_CURSO;
    }

    /** FINALIZADA, CANCELADA y NO_SHOW son terminales. */
    public boolean esTerminal() {
        return !retieneDisponibilidad();
    }
}
