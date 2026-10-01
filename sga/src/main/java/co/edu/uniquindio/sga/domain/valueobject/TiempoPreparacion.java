package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Horas requeridas entre una salida y la siguiente entrada (L-13). En el Anexo A son 3 horas.
 *
 * Reglas: RN-20 y 3.3 (si el tiempo excede la ventana entre la hora de salida y la de entrada,
 * no se puede recibir una entrada el mismo día de una salida).
 */
public record TiempoPreparacion(int horas) {

    public TiempoPreparacion {
        if (horas < 0 || horas > 24) {
            throw new ReglaDominioException("L-13: el tiempo de preparación debe estar entre 0 y 24 horas");
        }
    }

    /** Ej.: salida 11:00, entrada 15:00 = ventana de 4 horas; 3 horas no la exceden. */
    public boolean excedeHora(LocalTime horaSalida, LocalTime horaEntrada) {
        if (horaSalida == null || horaEntrada == null) {
            throw new ReglaDominioException("RN-20: las horas de salida y entrada son obligatorias");
        }
        long minutosVentana = Duration.between(horaSalida, horaEntrada).toMinutes();
        return horas * 60L > minutosVentana;
    }
}
