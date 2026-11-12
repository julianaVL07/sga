package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

import java.time.LocalDateTime;

/**
 * Respuesta al crear un canal o regenerar su credencial (10.3.1). Es la única vez que la credencial sale en claro:
 * el SGA solo guarda su hash y no la puede volver a mostrar.
 *
 * @param credencial valor del encabezado X-Canal-Credencial que la plataforma debe enviar en cada petición.
 */
public record CredencialCanalResponse(
        String nombre,
        CanalOrigen tipo,
        boolean activo,
        LocalDateTime fechaCreacion,
        String credencial
) {
}
