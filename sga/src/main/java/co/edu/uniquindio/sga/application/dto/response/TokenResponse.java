package co.edu.uniquindio.sga.application.dto.response;

import java.time.Instant;
import java.util.List;

/** DTO de salida del inicio de sesión (Guía 12) */
public record TokenResponse(
        String token,
        String tipo, // siempre "Bearer"
        Instant expiraEn,
        List<String> roles
) {
}
