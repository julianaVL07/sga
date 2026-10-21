// src/main/java/co/edu/uniquindio/sga/application/dto/request/RegistrarBloqueoRequest.java
package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * CU-13. POST /api/apartamentos/{id}/bloqueos
 * Retira el apartamento de la venta en un rango de fechas (7.3). fechaFin es la primera noche que ya no queda
 * bloqueada (rango semiabierto, 3.1). Que el rango sea válido y no choque con reservas activas lo valida el dominio.
 */
public record RegistrarBloqueoRequest(
        @NotNull(message = "La fecha de inicio del bloqueo es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin del bloqueo es obligatoria")
        LocalDate fechaFin,

        @NotBlank(message = "El motivo del bloqueo es obligatorio")
        String motivo
) {
}