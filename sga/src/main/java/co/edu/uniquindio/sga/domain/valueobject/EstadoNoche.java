package co.edu.uniquindio.sga.domain.valueobject;

/**
 * Estado de una noche del calendario de ocupación de un apartamento (CU-27, 7.1).
 * RESERVADA: la ocupa una reserva activa (RN-12). BLOQUEADA: la afecta un bloqueo vigente (RN-07).
 */
public enum EstadoNoche {
    LIBRE,
    RESERVADA,
    BLOQUEADA
}
