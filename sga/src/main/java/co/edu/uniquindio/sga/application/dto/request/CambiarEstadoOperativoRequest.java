package co.edu.uniquindio.sga.application.dto.request;

import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import jakarta.validation.constraints.NotNull;

/**
 * CU-16. PUT /api/apartamentos/{id}/estado-operativo
 * Nuevo estado operativo del apartamento: PREPARADO, OCUPADO, PENDIENTE_PREPARACION, EN_PREPARACION o
 * FUERA_DE_SERVICIO. Si la transición está permitida lo decide Apartamento (409 si no).
 */
public record CambiarEstadoOperativoRequest(
        @NotNull(message = "El nuevo estado operativo es obligatorio")
        EstadoOperativo nuevoEstado
) {
}