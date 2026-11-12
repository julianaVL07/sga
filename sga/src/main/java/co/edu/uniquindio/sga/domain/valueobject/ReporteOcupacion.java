package co.edu.uniquindio.sga.domain.valueobject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de ocupación de un periodo (7.9). Las fechas desde y hasta van incluidas: cada fecha es una noche.
 * porcentajeOcupacion = nochesVendidas / (apartamentosActivos × nochesPeriodo) × 100, con dos decimales.
 *
 * @param nochesDisponibles apartamentosActivos × nochesPeriodo, el denominador del porcentaje.
 * @param apartamentos      detalle por apartamento, ordenado por identificación.
 */
public record ReporteOcupacion(LocalDate desde, LocalDate hasta, int nochesPeriodo, int apartamentosActivos,
                               int nochesDisponibles, int nochesVendidas, BigDecimal porcentajeOcupacion,
                               List<OcupacionApartamento> apartamentos) {

    public ReporteOcupacion {
        apartamentos = List.copyOf(apartamentos);
    }

    /**
     * Noches vendidas de un apartamento en el periodo.
     *
     * @param porcentajeOcupacion nochesVendidas / nochesPeriodo × 100, con dos decimales.
     * @param activo              false si el apartamento vendió noches pero hoy no está a la venta (no cuenta en
     *                            el denominador del total).
     */
    public record OcupacionApartamento(IdentificacionApartamento apartamento, int nochesVendidas,
                                       BigDecimal porcentajeOcupacion, boolean activo) {
    }
}
