package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalTime;

/**
 * Condición en la que la llegada estimada ocurre a partir de la hora nocturna configurada (glosario, RP-03).
 * Reglas: RP-03 (genera recargo si la hora estimada es igual o posterior a la hora límite, 20:00 en el Anexo A),
 * RN-09 (la hora estimada debe existir).
 */
public record RegistroLlegadaNocturna(LocalTime horaEstimada) {

    public RegistroLlegadaNocturna {
        if (horaEstimada == null) {
            throw new ReglaDominioException("RN-09: la hora estimada de llegada es obligatoria");
        }
    }

    public boolean generaRecargo(LocalTime horaLimiteNocturna) {
        if (horaLimiteNocturna == null) {
            throw new ReglaDominioException("RP-03: la hora de llegada nocturna no está configurada");
        }
        return !horaEstimada.isBefore(horaLimiteNocturna);
    }
}
