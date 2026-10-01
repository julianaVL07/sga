package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.util.List;

/**
 * Valor de una estancia con su desglose noche por noche (7.4). La reserva la congela al crearse (3.5, RN-22).
 *
 * Reglas: RN-05 (el valor es la suma, noche por noche, de los subtotales).
 */
public record Cotizacion(List<DetalleNoche> detalles, Dinero valorTotal) {

    public Cotizacion {
        if (detalles == null || detalles.isEmpty()) {
            throw new ReglaDominioException("RN-05: la cotización debe tener al menos una noche");
        }
        detalles = List.copyOf(detalles);
        for (int i = 1; i < detalles.size(); i++) {
            if (!detalles.get(i).fecha().equals(detalles.get(i - 1).fecha().plusDays(1))) {
                throw new ReglaDominioException("RN-05: las noches de la cotización deben ser consecutivas");
            }
        }
        Dinero suma = sumar(detalles);
        if (valorTotal == null || !valorTotal.equals(suma)) {
            throw new ReglaDominioException("RN-05: el valor total debe ser la suma de los subtotales por noche");
        }
    }

    /** Crea la cotización calculando el total a partir del desglose. */
    public static Cotizacion desde(List<DetalleNoche> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ReglaDominioException("RN-05: la cotización debe tener al menos una noche");
        }
        return new Cotizacion(detalles, sumar(detalles));
    }

    public int totalNoches() {
        return detalles.size();
    }

    private static Dinero sumar(List<DetalleNoche> detalles) {
        return detalles.stream().map(DetalleNoche::subtotal).reduce(Dinero.cero(), Dinero::sumar);
    }
}
