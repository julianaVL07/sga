package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/apartamentos/novedades/sugerir-gravedad: la descripción de la novedad que se está escribiendo. */
public record SugerirGravedadRequest(
        @NotBlank(message = "La descripción de la novedad es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String descripcion
) {
}
