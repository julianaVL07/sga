package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.dto.request.CrearReservaRequest;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Verifica que el canal de una reserva nueva corresponda al rol de quien la crea (Guía 12, 7.1 y 7.5.3):
 * el huésped solo por PORTAL y a su nombre, la recepción solo por DIRECTO y el administrador por cualquiera de los dos.
 * Es una regla de acceso, no de negocio: depende de quién inició sesión y el dominio no conoce usuarios (3.6).
 */
@Component("canalPorRol")
public class CanalPorRolVerificador {

    public boolean puedeCrear(CrearReservaRequest request, Authentication authentication) {
        if (request == null || request.canalOrigen() == null || authentication == null) {
            return false;
        }
        CanalOrigen canal = request.canalOrigen();
        if (tieneRol(authentication, RolUsuario.ADMINISTRADOR)) {
            return canal == CanalOrigen.PORTAL || canal == CanalOrigen.DIRECTO;
        }
        if (tieneRol(authentication, RolUsuario.RECEPCIONISTA)) {
            return canal == CanalOrigen.DIRECTO;
        }
        if (tieneRol(authentication, RolUsuario.HUESPED)) {
            return canal == CanalOrigen.PORTAL && esASuNombre(request, authentication);
        }
        return false;
    }

    /** El documento del titular debe ser el del claim documento del token */
    private boolean esASuNombre(CrearReservaRequest request, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt) || request.titular() == null) {
            return false;
        }
        String documento = jwt.getClaimAsString("documento");
        return documento != null && documento.equals(request.titular().numeroDocumento());
    }

    private boolean tieneRol(Authentication authentication, RolUsuario rol) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rol.name()::equals);
    }
}
