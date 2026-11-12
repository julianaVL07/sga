package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

import java.time.LocalDateTime;

/** Canal de venta tal como lo ve el administrador (10.3.1). Nunca incluye la credencial ni su hash. */
public record CanalVentaResponse(
        String nombre,
        CanalOrigen tipo,
        boolean activo,
        LocalDateTime fechaCreacion
) {
}
