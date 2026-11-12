package co.edu.uniquindio.sga.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de ocupación (7.9). desde y hasta van incluidas. porcentajeOcupacion = nochesVendidas /
 * nochesDisponibles × 100, con dos decimales; nochesDisponibles = apartamentosActivos × nochesPeriodo.
 */
public record ReporteOcupacionResponse(
        LocalDate desde,
        LocalDate hasta,
        int nochesPeriodo,
        int apartamentosActivos,
        int nochesDisponibles,
        int nochesVendidas,
        BigDecimal porcentajeOcupacion,
        List<Apartamento> apartamentos
) {

    /** Noches vendidas de un apartamento; activo es false si hoy no está a la venta. */
    public record Apartamento(String apartamento, int nochesVendidas, BigDecimal porcentajeOcupacion,
                              boolean activo) {
    }
}
