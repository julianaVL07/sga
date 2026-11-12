package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * POST /api/canales. El administrador da de alta una plataforma externa; el SGA genera su credencial (10.3.1).
 * El nombre viaja después en la ruta /api/canales/{canal}: minúsculas, números y guiones.
 */
public record CrearCanalVentaRequest(
        @NotBlank(message = "El nombre del canal es obligatorio")
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,39}$",
                message = "De 2 a 40 caracteres entre minúsculas, números y guiones; empieza por letra o número")
        String nombre
) {
}
