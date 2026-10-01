package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Regla única del alojamiento que define retenciones según la antelación (F-07, 7.4). Es versionada:
 * cada reserva congela la versión vigente al crearse (RN-22).
 *
 * Reglas: RN-13 (la retención por cancelación y por no-show se calcula con esta versión congelada),
 * L-10 (mínimo dos tramos).
 */
public record PoliticaCancelacion(int version, LocalDate fechaVigencia, String consecuenciaNoShow,
                                  List<TramoCancelacion> tramos) {

    public PoliticaCancelacion {
        if (version < 1) {
            throw new ReglaDominioException("F-07: la versión de la política debe ser 1 o mayor");
        }
        if (fechaVigencia == null) {
            throw new ReglaDominioException("F-07: la política debe tener fecha de vigencia");
        }
        if (consecuenciaNoShow == null || consecuenciaNoShow.isBlank()) {
            throw new ReglaDominioException("7.4: la política debe definir qué ocurre ante un no-show");
        }
        if (tramos == null || tramos.size() < 2) {
            throw new ReglaDominioException("L-10: la política debe tener al menos dos tramos de antelación");
        }
        long antelacionesDistintas = tramos.stream().map(TramoCancelacion::antelacionMinimaDias).distinct().count();
        if (antelacionesDistintas != tramos.size()) {
            throw new ReglaDominioException("L-10: dos tramos no pueden tener la misma antelación mínima");
        }
        if (tramos.stream().noneMatch(t -> t.antelacionMinimaDias() == 0)) {
            throw new ReglaDominioException("L-10: debe existir un tramo desde 0 días para cubrir la última hora y el no-show");
        }
        tramos = tramos.stream()
                .sorted(Comparator.comparingInt(TramoCancelacion::antelacionMinimaDias).reversed())
                .toList();
    }

    /** El tramo que aplica es el de mayor antelación mínima que se cumple. */
    public int obtenerPorcentajeRetencion(int diasAntelacion) {
        return tramoPara(diasAntelacion).porcentajeRetencion();
    }

    /** RN-13: retención según los días entre la cancelación y la fecha de entrada. */
    public Dinero calcularRetencion(Dinero valorTotal, LocalDate fechaEntrada, LocalDate fechaCancelacion) {
        if (fechaEntrada == null || fechaCancelacion == null) {
            throw new ReglaDominioException("RN-13: las fechas de entrada y cancelación son obligatorias");
        }
        int dias = (int) Math.max(0, ChronoUnit.DAYS.between(fechaCancelacion, fechaEntrada));
        return tramoPara(dias).calcularRetencion(valorTotal);
    }

    /** RN-13: el no-show se liquida con el tramo de 0 días de antelación. */
    public Dinero calcularRetencionNoShow(Dinero valorTotal) {
        return tramoPara(0).calcularRetencion(valorTotal);
    }

    private TramoCancelacion tramoPara(int diasAntelacion) {
        return tramos.stream()
                .filter(t -> t.aplicaPara(diasAntelacion))
                .findFirst()
                .orElseThrow(() -> new ReglaDominioException("L-10: ningún tramo cubre " + diasAntelacion + " días"));
    }
}
