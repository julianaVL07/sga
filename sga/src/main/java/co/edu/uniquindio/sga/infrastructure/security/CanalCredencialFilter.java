package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.usecase.AutenticarCanalUseCase;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoAutenticacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Autentica al canal externo en las tres operaciones de CU-08 (10.3.1) con la credencial del encabezado
 * X-Canal-Credencial, que debe ser la de ESE canal ({canal} en la ruta) y el canal debe estar activo.
 * Sin credencial o con una que no corresponde responde 401; si el canal está inactivo, 403. En ambos casos con
 * ErrorResponse, y el intento queda en la bitácora (7.8) a través de AutenticarCanalUseCase.
 *
 * No es un bean: lo crea SecurityConfig dentro de la cadena del canal, para que Spring Boot no lo registre también
 * como filtro de todas las peticiones.
 */
public class CanalCredencialFilter extends OncePerRequestFilter {

    public static final String ENCABEZADO = "X-Canal-Credencial";

    /** Autoridad que recibe un canal autenticado; la exigen los métodos de CanalExternoController. */
    public static final String AUTORIDAD_CANAL = "CANAL_EXTERNO";

    /** Rutas de las tres operaciones, con cualquier método: son las que atiende la cadena de seguridad del canal. */
    public static final String[] RUTAS = {
            "/api/canales/*/disponibilidad",
            "/api/canales/*/reservas",
            "/api/canales/*/reservas/*/cancelar"
    };

    private record Operacion(RequestMatcher ruta, TipoEventoCanal tipo) {
    }

    private static final List<Operacion> OPERACIONES = List.of(
            new Operacion(PathPatternRequestMatcher.withDefaults()
                    .matcher(HttpMethod.GET, "/api/canales/{canal}/disponibilidad"),
                    TipoEventoCanal.CONSULTA_DISPONIBILIDAD),
            new Operacion(PathPatternRequestMatcher.withDefaults()
                    .matcher(HttpMethod.POST, "/api/canales/{canal}/reservas"),
                    TipoEventoCanal.CREACION_RESERVA),
            new Operacion(PathPatternRequestMatcher.withDefaults()
                    .matcher(HttpMethod.PUT, "/api/canales/{canal}/reservas/{identificadorExterno}/cancelar"),
                    TipoEventoCanal.CANCELACION_RESERVA));

    private final AutenticarCanalUseCase autenticarCanal;
    private final RespuestaErrorSeguridad respuestaError;
    private final SecurityContextHolderStrategy contexto = SecurityContextHolder.getContextHolderStrategy();

    public CanalCredencialFilter(AutenticarCanalUseCase autenticarCanal, RespuestaErrorSeguridad respuestaError) {
        this.autenticarCanal = autenticarCanal;
        this.respuestaError = respuestaError;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        for (Operacion operacion : OPERACIONES) {
            RequestMatcher.MatchResult coincidencia = operacion.ruta().matcher(request);
            if (coincidencia.isMatch()) {
                autenticar(request, response, chain, operacion.tipo(), coincidencia.getVariables());
                return;
            }
        }
        // Otro método sobre estas rutas: sigue sin autenticar y la autorización de la cadena responde 401.
        contexto.clearContext();
        chain.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                            TipoEventoCanal operacion, Map<String, String> variables)
            throws ServletException, IOException {
        String canal = variables.get("canal");
        String credencial = request.getHeader(ENCABEZADO);
        ResultadoAutenticacionCanal resultado = autenticarCanal.autenticar(canal, credencial, operacion,
                variables.get("identificadorExterno"));

        switch (resultado) {
            case AUTENTICADO -> {
                SecurityContext nuevo = contexto.createEmptyContext();
                nuevo.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        CanalVenta.normalizarNombre(canal), null,
                        List.of(new SimpleGrantedAuthority(AUTORIDAD_CANAL))));
                contexto.setContext(nuevo);
                chain.doFilter(request, response);
            }
            case CREDENCIAL_INVALIDA -> {
                contexto.clearContext();
                respuestaError.escribir(request, response, HttpStatus.UNAUTHORIZED,
                        credencial == null || credencial.isBlank()
                                ? "Falta la credencial del canal en el encabezado " + ENCABEZADO
                                : "La credencial no es válida para el canal " + canal);
            }
            case CANAL_INACTIVO -> {
                contexto.clearContext();
                respuestaError.escribir(request, response, HttpStatus.FORBIDDEN,
                        "El canal " + CanalVenta.normalizarNombre(canal) + " está inactivo");
            }
        }
    }
}
