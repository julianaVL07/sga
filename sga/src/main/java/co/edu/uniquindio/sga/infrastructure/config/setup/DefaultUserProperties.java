package co.edu.uniquindio.sga.infrastructure.config.setup;

import co.edu.uniquindio.sga.infrastructure.security.RolUsuario;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Usuarios iniciales leídos de application.properties (Guía 12, 12.3) */
@ConfigurationProperties(prefix = "sga.usuarios-iniciales")
public record DefaultUserProperties(List<DefaultUser> usuarios) {

    public record DefaultUser(
            String nombre,
            String correo,
            String contrasena,
            String telefono,
            String documento,
            String fechaNacimiento,
            RolUsuario rol
    ) {
    }
}
