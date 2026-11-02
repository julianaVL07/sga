package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * CU-30. PUT /api/politica-cancelacion
 * Nuevos tramos y consecuencia del no-show: crean la versión siguiente, vigente desde hoy (7.4). Este DTO solo
 * revisa la forma (400); L-10 (al menos dos tramos, antelaciones distintas y uno desde 0 días) lo valida el dominio
 * (409), por eso aquí no se exige un mínimo de tramos.
 */
public record ActualizarPoliticaCancelacionRequest(
        @NotBlank(message = "La consecuencia del no-show es obligatoria")
        String consecuenciaNoShow,

        @NotNull(message = "Los tramos son obligatorios")
        List<@Valid @NotNull(message = "El tramo no puede ser nulo") TramoCancelacionRequest> tramos
) {
}
