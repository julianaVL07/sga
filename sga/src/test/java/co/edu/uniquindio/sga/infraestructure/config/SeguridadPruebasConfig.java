package co.edu.uniquindio.sga.infraestructure.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Las pruebas de integración viajan autenticadas: toda petición de MockMvc lleva un token de administrador (Guía 12) */
@TestConfiguration
public class SeguridadPruebasConfig {

    @Bean
    MockMvcBuilderCustomizer tokenDeAdministrador() {
        return builder -> builder.defaultRequest(get("/")
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMINISTRADOR"))));
    }
}
