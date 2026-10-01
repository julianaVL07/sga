package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * Depósito de garantía reembolsable por dotación (RP-02): cargo de tipo ajuste que se asienta de manera
 * obligatoria en el folio al crear la reserva ($250.000 COP en el Anexo A). El monto es configuración.
 */
public record DepositoGarantia(String concepto, Dinero monto, boolean reembolsado) {

    public DepositoGarantia {
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaDominioException("RP-02: el depósito de garantía debe tener concepto");
        }
        if (monto == null || !monto.esPositivo()) {
            throw new ReglaDominioException("RP-02: el monto del depósito de garantía debe ser mayor que cero");
        }
        concepto = concepto.trim();
    }

    /** Depósito nuevo, aún no reembolsado. */
    public static DepositoGarantia de(String concepto, Dinero monto) {
        return new DepositoGarantia(concepto, monto, false);
    }

    /** RP-02: cargo de tipo ajuste que se asienta al abrir el folio. */
    public Cargo generarCargoAjuste(LocalDate fechaActual) {
        return new Cargo(TipoCargo.AJUSTE_MODIFICACION, concepto, monto, fechaActual);
    }

    /** Devolución registrada en el folio (el desembolso ocurre fuera del sistema, 3.4). */
    public Cargo generarDevolucion(LocalDate fechaActual) {
        if (reembolsado) {
            throw new ReglaDominioException("RP-02: el depósito de garantía ya fue reembolsado");
        }
        return new Cargo(TipoCargo.AJUSTE_MODIFICACION, "Devolución de " + concepto, monto.negar(), fechaActual);
    }

    public DepositoGarantia marcarReembolsado() {
        return new DepositoGarantia(concepto, monto, true);
    }
}
