package co.edu.uniquindio.sga.infrastructure.config.setup;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Carga un usuario por rol al iniciar (Guía 12, 12.3) */
@Component
@RequiredArgsConstructor
public class DefaultUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserInitializer.class);

    private final DefaultUserProperties props;
    private final UsuarioJpaDataRepository repository;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        props.usuarios().forEach(inicial -> {
            UsuarioEntity usuario = new UsuarioEntity();
            usuario.setNombre(inicial.nombre());
            usuario.setCorreo(inicial.correo());
            usuario.setContrasena(encoder.encode(inicial.contrasena()));
            usuario.setTelefono(inicial.telefono());
            usuario.setDocumento(inicial.documento());
            usuario.setFechaNacimiento(LocalDate.parse(inicial.fechaNacimiento()));
            usuario.setRol(inicial.rol());
            repository.save(usuario);
        });
        log.info("Usuarios iniciales cargados en H2: {}", repository.count());
    }
}
