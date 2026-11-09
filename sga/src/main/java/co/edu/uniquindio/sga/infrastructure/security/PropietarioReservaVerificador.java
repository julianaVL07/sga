package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Verifica que el usuario autenticado sea el titular de la reserva, y por tanto de su folio (Guía 12, 7.2).
 * Lee el dominio por su puerto: Reserva sigue sin saber que existen usuarios (3.6).
 */
@Component("propietarioReserva")
@RequiredArgsConstructor
public class PropietarioReservaVerificador {

    private final ReservaRepository reservaRepository;

    public boolean esTitular(String codigo, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        CodigoReserva codigoReserva;
        try {
            codigoReserva = new CodigoReserva(codigo);
        } catch (ReglaDominioException e) {
            return false; // código mal formado
        }
        String documento = jwt.getClaimAsString("documento");
        // Si la reserva no existe, también se niega: un huésped no puede
        // averiguar qué códigos de reserva existen probando con 403 frente a 404
        return reservaRepository.buscarPorCodigo(codigoReserva)
                .map(reserva -> reserva.getTitular().documento().numero().equals(documento))
                .orElse(false);
    }
}
