package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Ubicación exacta del alojamiento (7.3, L-02). Usa double porque no es dinero.
 */
public record Coordenada(double latitud, double longitud) {

    public Coordenada {
        if (latitud < -90 || latitud > 90) {
            throw new ReglaDominioException("7.3: la latitud debe estar entre -90 y 90");
        }
        if (longitud < -180 || longitud > 180) {
            throw new ReglaDominioException("7.3: la longitud debe estar entre -180 y 180");
        }
    }
}
