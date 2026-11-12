package co.edu.uniquindio.sga.domain.valueobject;

/**
 * Resultado de procesar un mensaje de un canal externo (7.8). Los dos últimos son intentos que no llegaron a
 * procesarse porque el canal no se autenticó (10.3.1): también quedan en la bitácora.
 */
public enum ResultadoEventoCanal {
    ACEPTADO,
    RECHAZADO_CONFLICTO,
    RECHAZADO_INVALIDO,
    DUPLICADO_IGNORADO,
    RECHAZADO_CREDENCIAL,
    RECHAZADO_CANAL_INACTIVO;

    /** RN-18: un conflicto se rechaza y se registra, nunca sobrescribe la reserva vigente. */
    public boolean generaConflicto() {
        return this == RECHAZADO_CONFLICTO;
    }

    /**
     * 10.3.1: el mensaje se rechazó antes de leerlo porque la credencial no sirve o el canal está inactivo.
     * Por eso puede quedar en la bitácora sin identificador externo.
     */
    public boolean esRechazoDeAcceso() {
        return this == RECHAZADO_CREDENCIAL || this == RECHAZADO_CANAL_INACTIVO;
    }
}
