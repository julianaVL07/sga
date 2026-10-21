package co.edu.uniquindio.sga.application.dto.request;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * CU-17. POST /api/apartamentos/{id}/novedades
 * Daño, faltante o incidencia del apartamento. La fecha la pone el SGA con el reloj de Colombia, no el cliente.
 */
public record RegistrarNovedadRequest(
        @NotBlank(message = "El autor de la novedad es obligatorio")
        String autor,

        @NotBlank(message = "La descripción de la novedad es obligatoria")
        String descripcion,

        @NotNull(message = "La gravedad de la novedad es obligatoria")
        GravedadNovedad gravedad                 // BAJA, MEDIA o ALTA
) {
}