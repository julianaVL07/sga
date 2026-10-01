package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Número máximo de personas que admite un apartamento. Tope rígido (F-03).
 * Reglas: RN-02 (el total de ocupantes no puede exceder la capacidad), RN-06 (los no facturables sí cuentan).
 */
public record Capacidad(int maximo) {

    public Capacidad {
        if (maximo < 1) {
            throw new ReglaDominioException("RN-02: la capacidad debe ser de al menos una persona");
        }
    }

    public boolean excede(int cantidadPersonas) {
        return cantidadPersonas > maximo;
    }
}
