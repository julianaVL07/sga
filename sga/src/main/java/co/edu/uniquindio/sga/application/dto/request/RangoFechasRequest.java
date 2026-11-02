package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * CU-29. Rango de fechas de una temporada con la fecha final excluida (nota de A.3): la temporada "del 1 al 10 de
 * marzo" se envía con fechaInicio 1-mar y fechaFin 11-mar. Que la fecha fin sea posterior a la de inicio y que el
 * rango no se solape con otro son reglas del dominio (409).
 * También es el cuerpo de POST /api/temporadas/{id}/rangos.
 */
public record RangoFechasRequest(
        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin
) {
}
