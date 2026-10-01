package co.edu.uniquindio.sga.domain.valueobject;

/** Operaciones que un canal externo puede invocar (10.3.1). */
public enum TipoEventoCanal {
    CONSULTA_DISPONIBILIDAD,
    CREACION_RESERVA,
    CANCELACION_RESERVA;

    /** RN-19: creación y cancelación siempre traen el identificador externo; la consulta no. */
    public boolean requiereIdentificadorExterno() {
        return this != CONSULTA_DISPONIBILIDAD;
    }
}
