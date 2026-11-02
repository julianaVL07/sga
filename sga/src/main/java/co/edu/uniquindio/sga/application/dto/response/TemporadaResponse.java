package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;

import java.time.LocalDate;
import java.util.List;

/**
 * CU-29. Temporada con sus rangos de fechas (fecha final excluida, nota de A.3). La base no tiene rangos: cubre todas
 * las fechas que no pertenecen a otra temporada (F-05).
 */
public record TemporadaResponse(
        String id,
        String nombre,
        TipoTemporada tipo,
        List<RangoResponse> rangos
) {

    public record RangoResponse(LocalDate fechaInicio, LocalDate fechaFin) {
    }
}
