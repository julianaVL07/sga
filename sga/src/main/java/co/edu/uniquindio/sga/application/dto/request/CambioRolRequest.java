package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** DTO de entrada de PUT /api/usuarios/{id}/rol (Guía 12, 7.2). Solo lo usa el administrador */
public record CambioRolRequest(
        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = ROLES, message = "El rol debe ser HUESPED, RECEPCIONISTA, ADMINISTRADOR o PERSONAL_SERVICIO")
        String rol
) {
    /** Roles del SGA (7.1), como texto: RolUsuario es de infraestructura */
    public static final String ROLES = "HUESPED|RECEPCIONISTA|ADMINISTRADOR|PERSONAL_SERVICIO";
}
