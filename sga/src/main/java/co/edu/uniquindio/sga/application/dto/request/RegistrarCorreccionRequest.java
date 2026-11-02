package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/reservas/{codigo}/folio/correcciones (RN-16: toda corrección es un movimiento inverso)
 * indiceCargo es la posición del cargo en la lista "cargos" de FolioResponse, contando desde 0. Que exista un cargo
 * en esa posición y que el folio esté abierto son reglas del dominio (409).
 */
public record RegistrarCorreccionRequest(
        @NotNull(message = "La posición del cargo a corregir es obligatoria")
        Integer indiceCargo,

        @NotBlank(message = "El motivo de la corrección es obligatorio")
        String motivo
) {
}
