package co.edu.uniquindio.sga.application.dto.request;

import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * CU-29. POST /api/temporadas
 * Temporada nueva con sus rangos y la tarifa de cada apartamento activo (7.4). Este DTO solo revisa la forma (400):
 * que una temporada no base tenga rangos, que no se solape, que no haya una segunda base y que ningún apartamento
 * activo quede sin tarifa lo valida el dominio (409). Por eso las listas pueden venir vacías; la base no lleva rangos.
 */
public record CrearTemporadaRequest(
        @NotBlank(message = "El id de la temporada es obligatorio")
        String id,

        @NotBlank(message = "El nombre de la temporada es obligatorio")
        String nombre,

        @NotNull(message = "El tipo de temporada es obligatorio")
        TipoTemporada tipo,

        List<@Valid @NotNull(message = "El rango no puede ser nulo") RangoFechasRequest> rangos,

        List<@Valid @NotNull(message = "La tarifa no puede ser nula") TarifaTemporadaRequest> tarifas
) {
}
