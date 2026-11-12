package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.usecase.AutenticarCanalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Cadenas de filtros de seguridad del SGA (Guía 12). Son dos:
 * <ol>
 *   <li>La del canal externo (10.3.1): solo las tres operaciones /api/canales/{canal}/..., autenticadas con la
 *   credencial del canal (CanalCredencialFilter), sin JWT. Va primero para que la otra no las atienda.</li>
 *   <li>La del JWT: todas las demás rutas, incluida la administración de los canales.</li>
 * </ol>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final RespuestaErrorSeguridad respuestaError;
    private final CorsConfigurationSource corsConfigurationSource;

    /** 10.3.1: las tres operaciones del canal externo se autentican con la credencial del canal, no con el JWT. */
    @Bean
    @Order(1)
    public SecurityFilterChain canalExternoFilterChain(HttpSecurity http, AutenticarCanalUseCase autenticarCanal)
            throws Exception {
        return http
                .securityMatcher(CanalCredencialFilter.RUTAS)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new CanalCredencialFilter(autenticarCanal, respuestaError), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority(CanalCredencialFilter.AUTORIDAD_CANAL))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, ex) -> respuestaError.escribir(request,
                                response, HttpStatus.UNAUTHORIZED, "Falta la credencial del canal en el encabezado "
                                        + CanalCredencialFilter.ENCABEZADO))
                        .accessDeniedHandler(accessDeniedHandler()))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS antes de la autenticación: el preflight OPTIONS del navegador no lleva token
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Permite que la consola de H2 se muestre en un iframe del mismo origen
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/registro",
                                "/api/auth/recuperacion/solicitar",
                                "/api/auth/recuperacion/restablecer",
                                "/error",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/h2-console/**"
                        ).permitAll()
                        // Mismo patrón que GET /api/apartamentos/{id}: se declara antes para que siga pidiendo token
                        .requestMatchers(HttpMethod.GET, "/api/apartamentos/pendientes-preparacion").authenticated()
                        // Consultas públicas del visitante: solo lectura y cotización, sin token
                        .requestMatchers(HttpMethod.GET,
                                "/api/apartamentos",
                                "/api/apartamentos/disponibles",
                                "/api/apartamentos/{id}",
                                "/api/alojamiento",
                                "/api/politica-cancelacion",
                                "/api/temporadas",
                                "/api/temporadas/{id}"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cotizaciones").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 y 403 que nacen en la cadena de filtros, antes de llegar al GlobalExceptionHandler (7.5.6)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    /** 401: sin token, token mal firmado o vencido. Conserva el encabezado WWW-Authenticate: Bearer */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
        return (request, response, ex) -> {
            bearer.commence(request, response, ex);
            respuestaError.escribir(request, response, HttpStatus.UNAUTHORIZED,
                    "Debe iniciar sesión o el token no es válido");
        };
    }

    /** 403: autenticado, pero sin permiso para la ruta */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> respuestaError.escribir(request, response, HttpStatus.FORBIDDEN,
                "No tiene permiso para realizar esta operación");
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
