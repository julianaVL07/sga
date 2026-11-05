package co.edu.uniquindio.sga.infrastructure.security;

/** Roles del SGA (7.1). No es un concepto del dominio: el dominio no conoce al usuario (3.6) */
public enum RolUsuario {
    HUESPED,
    RECEPCIONISTA,
    ADMINISTRADOR,
    PERSONAL_SERVICIO
}
