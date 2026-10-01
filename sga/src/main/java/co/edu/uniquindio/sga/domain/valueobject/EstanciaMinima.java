package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Estancia mínima (glosario del equipo, RP-01): noches mínimas cuando la reserva toca
 * un viernes o sábado en temporada media o alta. En el Anexo A son 2 noches.
 */
public record EstanciaMinima(int nochesMinimas, boolean aplicaTemporadaAltaMedia) {

    public EstanciaMinima {
        if (nochesMinimas < 1) {
            throw new ReglaDominioException("RP-01: la estancia mínima debe ser de al menos una noche");
        }
    }

    /** RP-01: solo se exige si la regla está activa y la estancia toca fin de semana en media o alta. */
    public boolean cumpleRegla(int nochesEstancia, boolean esAltaOMedia) {
        if (!aplicaTemporadaAltaMedia || !esAltaOMedia) {
            return true;
        }
        return nochesEstancia >= nochesMinimas;
    }
}
