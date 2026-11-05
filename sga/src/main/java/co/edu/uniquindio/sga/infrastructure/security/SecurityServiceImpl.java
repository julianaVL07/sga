package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.dto.request.LoginRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistroUsuarioRequest;
import co.edu.uniquindio.sga.application.dto.response.TokenResponse;
import co.edu.uniquindio.sga.application.dto.response.UsuarioRegistradoResponse;
import co.edu.uniquindio.sga.application.exception.UsuarioYaRegistradoException;
import co.edu.uniquindio.sga.application.security.SecurityService;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Implementación del inicio de sesión y del registro con Spring Security (Guía 12) */
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioJpaDataRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    @Value("${sga.jwt.expiracion-minutos}")
    private long expiracionMinutos;

    @Override
    public TokenResponse login(LoginRequest request) {
        // 1. Busca el usuario (UserConfig) y compara la contraseña con BCrypt.
        //    Si falla, lanza una AuthenticationException → 401 (sección 6.4)
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.correo(), request.contrasena())
        );

        // 2. Credenciales correctas: se leen el rol y el documento del usuario autenticado
        CustomUserDetails usuario = (CustomUserDetails) authentication.getPrincipal();
        // Del usuario, no de authentication: Spring Security 7 agrega ahí FACTOR_PASSWORD, que no es un rol del SGA
        List<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // 3. Se calcula la vigencia y se firma el token
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(expiracionMinutos, ChronoUnit.MINUTES);
        String token = jwtTokenProvider.generateTokenAsString(
                authentication.getName(), usuario.usuario().getDocumento(), roles, ahora, expira);

        return new TokenResponse(token, "Bearer", expira, roles);
    }

    @Override
    public UsuarioRegistradoResponse registrar(RegistroUsuarioRequest request) {
        if (usuarios.existsByCorreo(request.correo()) || usuarios.existsByDocumento(request.documento())) {
            throw new UsuarioYaRegistradoException();
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombre(request.nombre());
        usuario.setCorreo(request.correo());
        usuario.setContrasena(passwordEncoder.encode(request.contrasena()));
        usuario.setTelefono(request.telefono());
        usuario.setDocumento(request.documento());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setRol(RolUsuario.HUESPED); // los roles internos solo los asigna un administrador (7.5.2)

        try {
            UsuarioEntity guardado = usuarios.save(usuario);
            return new UsuarioRegistradoResponse(guardado.getId(), guardado.getNombre(), guardado.getCorreo(),
                    guardado.getRol().name());
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos pasan la consulta de arriba: decide la restricción única de la tabla
            throw new UsuarioYaRegistradoException();
        }
    }
}
