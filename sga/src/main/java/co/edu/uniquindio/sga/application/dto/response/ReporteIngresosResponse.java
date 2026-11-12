package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de ingresos (7.9): pagos con fecha entre desde y hasta (incluidas) por medio de pago. Los cargos no
 * cuentan. Los valores van en pesos enteros.
 */
public record ReporteIngresosResponse(
        LocalDate desde,
        LocalDate hasta,
        long total,
        int cantidadPagos,
        List<Medio> medios
) {

    /** Una fila por medio de pago; porcentaje es su participación en el total, con dos decimales. */
    public record Medio(MedioPago medio, int cantidadPagos, long total, BigDecimal porcentaje) {
    }
}
