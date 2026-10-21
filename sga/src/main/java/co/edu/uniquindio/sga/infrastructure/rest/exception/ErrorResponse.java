package co.edu.uniquindio.sga.infrastructure.rest.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo único de error de la API del SGA (Guía 07, 4.6). Todos los controladores responden con este formato:
 * 400 el mensaje está mal escrito, 404 el identificador no existe, 409 el negocio no lo permite (RN/RP).
 *
 * @param error   frase estándar del código HTTP, por ejemplo "Conflict".
 * @param mensaje para un 409 es el mensaje de la regla, por ejemplo "RN-09: ...".
 * @param path    ruta pedida, por ejemplo "/api/reservas/RES-2026-00001/confirmar".
 * @param errores campo → mensaje; solo viene en los 400 de validación, en los demás es null.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        Map<String, String> errores
) {
}
