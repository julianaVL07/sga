package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;

import java.time.LocalDate;

/**
 * Un cargo del folio (7.7). Es inmutable (RN-16); puede ser negativo si es un ajuste a la baja (RN-14) o un
 * movimiento inverso. El valor va en pesos enteros.
 */
public record CargoResponse(
        TipoCargo tipo,
        String concepto,
        long valor,
        LocalDate fecha
) {
}