package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.domain.port.CredencialesCanal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Adaptador del puerto CredencialesCanal (10.3.1): genera la credencial con SecureRandom (32 bytes, 256 bits, en
 * Base64 para URL, 43 caracteres) y la cifra con BCrypt. Solo el hash llega a la base de datos.
 */
@Component
public class CredencialesCanalBCrypt implements CredencialesCanal {

    private static final int BYTES_CREDENCIAL = 32;

    private final SecureRandom aleatorio = new SecureRandom();
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String generar() {
        byte[] bytes = new byte[BYTES_CREDENCIAL];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String cifrar(String credencial) {
        return bcrypt.encode(credencial);
    }

    @Override
    public boolean coincide(String credencial, String hash) {
        if (credencial == null || hash == null) {
            return false;
        }
        return bcrypt.matches(credencial, hash);
    }
}
