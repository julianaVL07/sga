package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.dto.request.RestablecerContrasenaRequest;
import co.edu.uniquindio.sga.application.dto.request.SolicitarRecuperacionRequest;
import co.edu.uniquindio.sga.application.event.CodigoRecuperacionGeneradoEvento;
import co.edu.uniquindio.sga.application.exception.CodigoRecuperacionInvalidoException;
import co.edu.uniquindio.sga.application.security.RecuperacionContrasenaService;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.CodigoRecuperacionJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.UsuarioJpaDataRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CodigoRecuperacionEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Recuperación de contraseña (7.2) con Spring Data y Spring Security.
 * El código (6 dígitos al azar) se guarda cifrado y se envía por el puerto NotificadorTitular, después de guardar
 * y en otro hilo, igual que las notificaciones de las reservas: así la respuesta tarda lo mismo exista o no la
 * cuenta y un fallo del correo no se le muestra a nadie.
 */
@Service
public class RecuperacionContrasenaServiceImpl implements RecuperacionContrasenaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionContrasenaServiceImpl.class);
    private static final SecureRandom AZAR = new SecureRandom();

    private final UsuarioJpaDataRepository usuarios;
    private final CodigoRecuperacionJpaDataRepository codigos;
    private final PasswordEncoder cifrador;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;
    private final Duration vigencia;
    private final int intentosMaximos;

    public RecuperacionContrasenaServiceImpl(UsuarioJpaDataRepository usuarios,
                                             CodigoRecuperacionJpaDataRepository codigos,
                                             PasswordEncoder cifrador, ApplicationEventPublisher eventos, Clock reloj,
                                             @Value("${sga.recuperacion.vigencia}") Duration vigencia,
                                             @Value("${sga.recuperacion.intentos-maximos}") int intentosMaximos) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.codigos = Objects.requireNonNull(codigos);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.eventos = Objects.requireNonNull(eventos);
        this.reloj = Objects.requireNonNull(reloj);
        this.vigencia = Objects.requireNonNull(vigencia);
        this.intentosMaximos = intentosMaximos;
    }

    @Override
    @Transactional
    public void solicitar(SolicitarRecuperacionRequest request) {
        String codigo = generarCodigo();
        Optional<UsuarioEntity> usuario = buscarActivo(request.correo());
        if (usuario.isEmpty()) {
            // Se cifra igual, para que la respuesta tarde lo mismo que con una cuenta real
            cifrador.encode(codigo);
            log.info("Recuperación: se pidió un código para un correo sin cuenta activa; no se envía nada");
            return;
        }
        UsuarioEntity cuenta = usuario.get();
        CodigoRecuperacionEntity registro = codigos.findByCorreo(cuenta.getCorreo())
                .orElseGet(() -> new CodigoRecuperacionEntity(cuenta.getCorreo()));
        registro.emitir(codigo, Instant.now(reloj), vigencia, cifrador);
        codigos.save(registro);
        eventos.publishEvent(new CodigoRecuperacionGeneradoEvento(cuenta.getCorreo(), cuenta.getNombre(), codigo,
                vigencia.toMinutes()));
    }

    /**
     * No deshace la transacción al rechazar el código: el intento fallido debe quedar guardado para que los
     * intentos se agoten de verdad.
     */
    @Override
    @Transactional(noRollbackFor = CodigoRecuperacionInvalidoException.class)
    public void restablecer(RestablecerContrasenaRequest request) {
        UsuarioEntity usuario = buscarActivo(request.correo()).orElseThrow(CodigoRecuperacionInvalidoException::new);
        CodigoRecuperacionEntity registro = codigos.findByCorreo(usuario.getCorreo())
                .orElseThrow(CodigoRecuperacionInvalidoException::new);
        if (!registro.verificar(request.codigo(), Instant.now(reloj), intentosMaximos, cifrador)) {
            throw new CodigoRecuperacionInvalidoException();
        }
        usuario.setContrasena(cifrador.encode(request.nuevaContrasena()));
        log.info("Recuperación: contraseña restablecida para el usuario {}", usuario.getId());
    }

    private Optional<UsuarioEntity> buscarActivo(String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        return usuarios.findByCorreo(correo.trim()).filter(UsuarioEntity::isActivo);
    }

    /** Seis dígitos al azar, con ceros a la izquierda: de 000000 a 999999. */
    static String generarCodigo() {
        return String.format("%06d", AZAR.nextInt(1_000_000));
    }
}
