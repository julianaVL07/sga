package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * Una línea del desglose noche por noche de la cotización (7.4): fecha, temporada, tarifa aplicada,
 * ocupantes facturables y subtotal.
 *
 * Reglas: RN-05 (subtotal = tarifa de esa noche × ocupantes facturables), RN-22 (queda congelado en la reserva).
 */
public record DetalleNoche(LocalDate fecha, String temporada, Dinero tarifaAplicada, int ocupantesFacturables,
                           Dinero subtotal) {

    public DetalleNoche {
        if (fecha == null) {
            throw new ReglaDominioException("RN-05: el detalle debe indicar la fecha de la noche");
        }
        if (temporada == null || temporada.isBlank()) {
            throw new ReglaDominioException("RN-05: el detalle debe indicar la temporada de la noche");
        }
        if (tarifaAplicada == null || subtotal == null) {
            throw new ReglaDominioException("RN-05: el detalle debe tener tarifa y subtotal");
        }
        if (ocupantesFacturables < 0) {
            throw new ReglaDominioException("RN-05: los ocupantes facturables no pueden ser negativos");
        }
        if (!subtotal.equals(tarifaAplicada.multiplicar(ocupantesFacturables))) {
            throw new ReglaDominioException("RN-05: el subtotal debe ser la tarifa por los ocupantes facturables");
        }
    }

    /** Calcula la noche a partir de la tarifa vigente del apartamento en la temporada de esa noche. */
    public static DetalleNoche calcular(LocalDate fecha, String temporada, Tarifa tarifa, int ocupantesFacturables) {
        if (tarifa == null) {
            throw new ReglaDominioException("RN-05: la tarifa de la noche es obligatoria");
        }
        return new DetalleNoche(fecha, temporada, tarifa.valorPorOcupante(), ocupantesFacturables,
                tarifa.calcularSubtotalNoche(ocupantesFacturables));
    }
}
