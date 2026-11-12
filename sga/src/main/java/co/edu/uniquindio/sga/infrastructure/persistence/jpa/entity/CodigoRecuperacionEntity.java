package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Código para restablecer la contraseña (7.2). Como el usuario, es un concepto de acceso y vive en infraestructura.
 * Hay a lo sumo uno por correo: pedir otro reemplaza al anterior. El código se guarda cifrado (como las
 * contraseñas), vence a los sga.recuperacion.vigencia, sirve una sola vez y se anula tras
 * sga.recuperacion.intentos-maximos intentos fallidos, para que no se pueda adivinar probando.
 */
@Entity
@Table(name = "codigo_recuperacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodigoRecuperacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash; // cifrado con el PasswordEncoder, nunca en texto plano

    @Column(nullable = false)
    private Instant venceEn;

    @Column(nullable = false)
    private int intentosFallidos;

    @Column(nullable = false)
    private boolean usado;

    public CodigoRecuperacionEntity(String correo) {
        this.correo = Objects.requireNonNull(correo);
    }

    /** Deja un código nuevo: cifra el que se va a enviar, fija el vencimiento y reinicia los intentos. */
    public void emitir(String codigo, Instant ahora, Duration vigencia, PasswordEncoder cifrador) {
        this.codigoHash = cifrador.encode(Objects.requireNonNull(codigo));
        this.venceEn = ahora.plus(vigencia);
        this.intentosFallidos = 0;
        this.usado = false;
    }

    /** Se puede usar si no se ha usado, no ha vencido y no se agotaron los intentos. */
    public boolean estaVigente(Instant ahora, int intentosMaximos) {
        return codigoHash != null && !usado && ahora.isBefore(venceEn) && intentosFallidos < intentosMaximos;
    }

    /**
     * Comprueba el código. Si coincide y está vigente, queda usado (sirve una sola vez) y responde true.
     * Si no coincide, cuenta un intento fallido. Si ya no estaba vigente, responde false sin más.
     */
    public boolean verificar(String codigo, Instant ahora, int intentosMaximos, PasswordEncoder cifrador) {
        if (!estaVigente(ahora, intentosMaximos)) {
            return false;
        }
        if (codigo == null || !cifrador.matches(codigo, codigoHash)) {
            intentosFallidos++;
            return false;
        }
        usado = true;
        return true;
    }
}
