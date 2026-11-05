package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Spring Security busca los usuarios en H2 (Guía 12) */
@Configuration
public class UserConfig {

    @Bean
    public UserDetailsService userDetailsServiceFromDataBase(UsuarioJpaDataRepository repository) {
        return correo -> repository.findByCorreo(correo)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente"));
    }
}
