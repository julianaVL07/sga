package co.edu.uniquindio.sga.domain.valueobject;

/**
 * Condición física presente del apartamento (3.3, F-09). Las transiciones permitidas (7.6)
 * las valida Apartamento.
 */
public enum EstadoOperativo {
    PREPARADO,
    OCUPADO,
    PENDIENTE_PREPARACION,
    EN_PREPARACION,
    FUERA_DE_SERVICIO;

    /** RN-11: solo un apartamento PREPARADO puede recibir un grupo. */
    public boolean permiteRegistro() {
        return this == PREPARADO;
    }

    /** Estados que el personal de servicio debe atender (7.1). */
    public boolean requiereLimpieza() {
        return this == PENDIENTE_PREPARACION || this == EN_PREPARACION;
    }
}
