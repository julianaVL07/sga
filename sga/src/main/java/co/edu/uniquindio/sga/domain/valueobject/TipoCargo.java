package co.edu.uniquindio.sga.domain.valueobject;

/** Tipos de cargo del folio (7.7). */
public enum TipoCargo {
    ALOJAMIENTO,
    SERVICIO_ADICIONAL,
    /**
     * RN-14: diferencia de valor al modificar una reserva. RP-02: también es el tipo del depósito de garantía y de su
     * devolución (valor negativo), como lo definen el Anexo A, el glosario y 7.7.
     */
    AJUSTE_MODIFICACION,
    PENALIDAD_CANCELACION_NOSHOW
}
