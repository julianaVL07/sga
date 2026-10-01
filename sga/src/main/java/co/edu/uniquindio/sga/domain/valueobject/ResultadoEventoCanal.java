package co.edu.uniquindio.sga.domain.valueobject;

/** Resultado de procesar un mensaje de un canal externo (7.8). */
public enum ResultadoEventoCanal {
    ACEPTADO,
    RECHAZADO_CONFLICTO,
    RECHAZADO_INVALIDO,
    DUPLICADO_IGNORADO;

    /** RN-18: un conflicto se rechaza y se registra, nunca sobrescribe la reserva vigente. */
    public boolean generaConflicto() {
        return this == RECHAZADO_CONFLICTO;
    }
}
