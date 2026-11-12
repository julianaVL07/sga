package co.edu.uniquindio.sga.domain.valueobject;

/**
 * Resultado de autenticar a un canal externo con su credencial (10.3.1). Solo AUTENTICADO deja pasar el mensaje;
 * los otros dos quedan en la bitácora (7.8).
 */
public enum ResultadoAutenticacionCanal {
    AUTENTICADO,
    /** Sin credencial, credencial incorrecta o canal que no existe. */
    CREDENCIAL_INVALIDA,
    /** La credencial es correcta pero el administrador desactivó el canal. */
    CANAL_INACTIVO;

    public boolean autenticado() {
        return this == AUTENTICADO;
    }
}
