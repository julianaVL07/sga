package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetario en pesos colombianos (COP), sin decimales (3.4).
 * Usa BigDecimal: está prohibido float o double para dinero.
 * Admite valores negativos porque los ajustes y los movimientos inversos pueden restar (RN-14, RN-16).
 * El redondeo al peso más cercano se aplica solo al final del cálculo de un cargo (porcentaje).
 */
public record Dinero(BigDecimal monto) {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    public Dinero {
        if (monto == null) {
            throw new ReglaDominioException("3.4: el monto es obligatorio");
        }
        if (monto.stripTrailingZeros().scale() > 0) {
            throw new ReglaDominioException("3.4: el peso colombiano no maneja decimales: " + monto);
        }
        monto = monto.setScale(0, RoundingMode.UNNECESSARY);
    }

    public static Dinero cero() {
        return new Dinero(BigDecimal.ZERO);
    }

    public static Dinero de(long pesos) {
        return new Dinero(BigDecimal.valueOf(pesos));
    }

    public Dinero sumar(Dinero otro) {
        return new Dinero(monto.add(requerido(otro).monto));
    }

    public Dinero restar(Dinero otro) {
        return new Dinero(monto.subtract(requerido(otro).monto));
    }

    public Dinero multiplicar(int factor) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(factor)));
    }

    /** Valor opuesto; se usa para movimientos inversos (RN-16). */
    public Dinero negar() {
        return new Dinero(monto.negate());
    }

    /** Porcentaje del valor, redondeado al peso más cercano (3.4). Ej.: retención de la política (RN-13). */
    public Dinero porcentaje(int porcentaje) {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new ReglaDominioException("3.4: el porcentaje debe estar entre 0 y 100");
        }
        BigDecimal resultado = monto.multiply(BigDecimal.valueOf(porcentaje))
                .divide(CIEN, 0, RoundingMode.HALF_UP);
        return new Dinero(resultado);
    }

    public boolean esCero() {
        return monto.signum() == 0;
    }

    public boolean esNegativo() {
        return monto.signum() < 0;
    }

    public boolean esPositivo() {
        return monto.signum() > 0;
    }

    public boolean esMayorOIgualQue(Dinero otro) {
        return monto.compareTo(requerido(otro).monto) >= 0;
    }

    private static Dinero requerido(Dinero otro) {
        if (otro == null) {
            throw new ReglaDominioException("3.4: el valor a operar es obligatorio");
        }
        return otro;
    }
}
