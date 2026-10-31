package co.edu.uniquindio.sga.domain.valueobject;

/** Tipos de cargo del folio (7.7). */
public enum TipoCargo {
    ALOJAMIENTO,
    SERVICIO_ADICIONAL,
    /** RN-14: diferencia de valor al modificar una reserva. */
    AJUSTE_MODIFICACION,
    PENALIDAD_CANCELACION_NOSHOW,
    /** RP-02: depósito de garantía reembolsable (y su devolución, con valor negativo). */
    DEPOSITO_GARANTIA
}
