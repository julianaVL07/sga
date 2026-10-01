package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Valor por ocupante facturable, por noche, para un apartamento en una temporada (glosario, 7.4).
 * La guarda el Apartamento; aquí solo se referencia la temporada por su id.
 *
 * Reglas: RN-05 (subtotal de una noche = tarifa × ocupantes facturables).
 */
public record Tarifa(String idTemporada, Dinero valorPorOcupante) {

    public Tarifa {
        if (idTemporada == null || idTemporada.isBlank()) {
            throw new ReglaDominioException("7.4: la tarifa debe indicar la temporada");
        }
        if (valorPorOcupante == null || !valorPorOcupante.esPositivo()) {
            throw new ReglaDominioException("7.4: el valor de la tarifa debe ser mayor que cero");
        }
    }

    public Dinero calcularSubtotalNoche(int ocupantesFacturables) {
        if (ocupantesFacturables < 0) {
            throw new ReglaDominioException("RN-05: los ocupantes facturables no pueden ser negativos");
        }
        return valorPorOcupante.multiplicar(ocupantesFacturables);
    }
}
