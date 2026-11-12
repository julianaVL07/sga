package co.edu.uniquindio.sga.domain.valueobject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de reservas por canal de origen (7.9): reservas con fecha de entrada entre desde y hasta (ambas
 * incluidas), en cualquier estado, también las canceladas y los no-show.
 *
 * @param canales una fila por cada canal (PORTAL, DIRECTO, EXTERNO), aunque no tenga reservas en el periodo.
 */
public record ReporteReservasPorCanal(LocalDate desde, LocalDate hasta, int totalReservas,
                                      List<ReservasDeCanal> canales) {

    public ReporteReservasPorCanal {
        canales = List.copyOf(canales);
    }

    /**
     * Reservas de un canal en el periodo.
     *
     * @param activas    PENDIENTE, CONFIRMADA o EN_CURSO.
     * @param porcentaje participación del canal en el total, con dos decimales (0 si no hay reservas).
     */
    public record ReservasDeCanal(CanalOrigen canal, int total, int activas, int finalizadas, int canceladas,
                                  int noShow, BigDecimal porcentaje) {
    }
}
