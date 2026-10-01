package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Identificación única y estable de un apartamento (L-04). Ej.: APT-101.
 * Se guarda en mayúsculas y sin espacios alrededor para que "apt-101" y "APT-101" sean la misma.
 */
public record IdentificacionApartamento(String codigo) {

    public IdentificacionApartamento {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaDominioException("L-04: la identificación del apartamento es obligatoria");
        }
        codigo = codigo.trim().toUpperCase();
    }
}
