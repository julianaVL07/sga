package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.time.LocalDate;

/**
 * Criterios del listado de reservas (CU-19, 7.5 "Listados"). Todos son opcionales: un campo en null no filtra.
 * Las fechas desde y hasta acotan la fecha de entrada de la estancia, ambas incluidas.
 */
public record FiltroReservas(EstadoReserva estado, IdentificacionApartamento apartamento, CanalOrigen canal,
                             String documentoTitular, LocalDate desde, LocalDate hasta) {

    public FiltroReservas {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaDominioException("7.5: la fecha desde (" + desde + ") no puede ser posterior a la fecha hasta ("
                    + hasta + ")");
        }
        documentoTitular = (documentoTitular == null || documentoTitular.isBlank()) ? null : documentoTitular.trim();
    }

    /** Sin ningún criterio: todas las reservas. */
    public static FiltroReservas sinFiltro() {
        return new FiltroReservas(null, null, null, null, null, null);
    }
}
