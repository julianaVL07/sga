package co.edu.uniquindio.sga.domain.valueobject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de ingresos de un periodo (7.9): suma de los PAGOS registrados en los folios con fecha entre desde y
 * hasta (ambas incluidas), por medio de pago. Los cargos no cuentan: son lo que se cobra, no lo que entró.
 *
 * @param medios una fila por cada medio de pago aceptado (L-18), aunque no tenga pagos en el periodo.
 */
public record ReporteIngresos(LocalDate desde, LocalDate hasta, Dinero total, int cantidadPagos,
                              List<IngresoPorMedio> medios) {

    public ReporteIngresos {
        medios = List.copyOf(medios);
    }

    /** @param porcentaje participación del medio en el total, con dos decimales (0 si el total es cero). */
    public record IngresoPorMedio(MedioPago medio, int cantidadPagos, Dinero total, BigDecimal porcentaje) {
    }
}
