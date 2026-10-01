package co.edu.uniquindio.sga.domain.valueobject;

/**
 * Origen de la reserva (glosario, 2.4, F-10). Los tres canales venden sobre el mismo inventario (RN-01).
 */
public enum CanalOrigen {
    PORTAL,
    DIRECTO,
    EXTERNO;

    /** RN-19: una reserva externa llega siempre con el identificador propio del canal. */
    public boolean exigeIdentificadorExterno() {
        return this == EXTERNO;
    }
}
