package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Edad desde la cual un ocupante genera cargo (L-09). En el Anexo A son 6 años.
 * Es configuración del alojamiento, nunca una constante en el código.
 *
 * Reglas: RN-06 (facturable si alcanza o supera el umbral).
 */
public record UmbralEdadFacturable(int anios) {

    public UmbralEdadFacturable {
        if (anios < 0 || anios > 120) {
            throw new ReglaDominioException("L-09: el umbral de edad facturable debe estar entre 0 y 120 años");
        }
    }

    public boolean esFacturable(int edadCalculada) {
        return edadCalculada >= anios;
    }
}
