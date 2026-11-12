package co.edu.uniquindio.sga.application.security;

import co.edu.uniquindio.sga.application.dto.request.RestablecerContrasenaRequest;
import co.edu.uniquindio.sga.application.dto.request.SolicitarRecuperacionRequest;

/** Puerto de la recuperación de contraseña con un código enviado al correo (7.2). */
public interface RecuperacionContrasenaService {

    /**
     * Genera un código de 6 dígitos y lo envía al correo si pertenece a una cuenta activa. Si no, no hace nada:
     * quien llama responde igual en los dos casos para no revelar qué cuentas existen.
     */
    void solicitar(SolicitarRecuperacionRequest request);

    /**
     * Cambia la contraseña si el código coincide, no ha vencido y no se ha usado.
     *
     * @throws co.edu.uniquindio.sga.application.exception.CodigoRecuperacionInvalidoException si no sirve.
     */
    void restablecer(RestablecerContrasenaRequest request);
}
