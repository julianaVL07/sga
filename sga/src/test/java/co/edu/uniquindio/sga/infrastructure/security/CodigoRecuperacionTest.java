package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CodigoRecuperacionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reglas del código para restablecer la contraseña (7.2), sin Spring ni base de datos: vence a los 15 minutos,
 * sirve una sola vez, un código incorrecto no sirve y se anula tras 5 intentos fallidos.
 */
class CodigoRecuperacionTest {

    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final int INTENTOS = 5;
    private static final Instant AHORA = Instant.parse("2026-11-12T15:00:00Z");

    // Costo mínimo de BCrypt: la prueba no mide la seguridad del cifrado sino las reglas del código
    private final PasswordEncoder cifrador = new BCryptPasswordEncoder(4);
    private CodigoRecuperacionEntity codigo;

    @BeforeEach
    void emitir() {
        codigo = new CodigoRecuperacionEntity("huesped1@sga.local");
        codigo.emitir("123456", AHORA, VIGENCIA, cifrador);
    }

    @Test
    @DisplayName("Se guarda cifrado, nunca el código en claro")
    void seGuardaCifrado() {
        assertNotEquals("123456", codigo.getCodigoHash());
        assertTrue(cifrador.matches("123456", codigo.getCodigoHash()));
    }

    @Test
    @DisplayName("El código correcto y vigente sirve")
    void codigoCorrectoSirve() {
        assertTrue(codigo.verificar("123456", AHORA.plus(Duration.ofMinutes(14)), INTENTOS, cifrador));
        assertTrue(codigo.isUsado());
    }

    @Test
    @DisplayName("Sirve una sola vez")
    void sirveUnaSolaVez() {
        assertTrue(codigo.verificar("123456", AHORA, INTENTOS, cifrador));
        assertFalse(codigo.verificar("123456", AHORA, INTENTOS, cifrador));
    }

    @Test
    @DisplayName("Vence a los 15 minutos: justo en el minuto 15 ya no sirve")
    void vence() {
        assertFalse(codigo.verificar("123456", AHORA.plus(VIGENCIA), INTENTOS, cifrador));
        assertFalse(codigo.verificar("123456", AHORA.plus(Duration.ofHours(2)), INTENTOS, cifrador));
        assertFalse(codigo.isUsado());
    }

    @Test
    @DisplayName("Un código incorrecto no sirve y cuenta un intento fallido")
    void codigoIncorrecto() {
        assertFalse(codigo.verificar("654321", AHORA, INTENTOS, cifrador));
        assertFalse(codigo.verificar(null, AHORA, INTENTOS, cifrador));
        assertEquals(2, codigo.getIntentosFallidos());
        assertFalse(codigo.isUsado());
        // Después de fallar, el correcto sigue sirviendo mientras queden intentos
        assertTrue(codigo.verificar("123456", AHORA, INTENTOS, cifrador));
    }

    @Test
    @DisplayName("Tras 5 intentos fallidos se anula: ni el correcto sirve")
    void seAnulaTrasLosIntentos() {
        for (int i = 0; i < INTENTOS; i++) {
            assertFalse(codigo.verificar("000000", AHORA, INTENTOS, cifrador));
        }
        assertFalse(codigo.estaVigente(AHORA, INTENTOS));
        assertFalse(codigo.verificar("123456", AHORA, INTENTOS, cifrador));
    }

    @Test
    @DisplayName("Pedir otro código reemplaza al anterior y reinicia los intentos")
    void pedirOtroReemplazaAlAnterior() {
        codigo.verificar("000000", AHORA, INTENTOS, cifrador);
        codigo.emitir("777777", AHORA.plus(Duration.ofMinutes(1)), VIGENCIA, cifrador);

        assertEquals(0, codigo.getIntentosFallidos());
        assertFalse(codigo.verificar("123456", AHORA.plus(Duration.ofMinutes(2)), INTENTOS, cifrador));
        assertTrue(codigo.verificar("777777", AHORA.plus(Duration.ofMinutes(2)), INTENTOS, cifrador));
    }

    @Test
    @DisplayName("El código generado tiene siempre 6 dígitos")
    void codigoDeSeisDigitos() {
        for (int i = 0; i < 200; i++) {
            assertTrue(RecuperacionContrasenaServiceImpl.generarCodigo().matches("\\d{6}"));
        }
    }
}
