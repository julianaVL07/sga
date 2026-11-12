package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de reservas por canal (7.9): reservas con fecha de entrada entre desde y hasta (incluidas), en
 * cualquier estado, también las canceladas y los no-show.
 */
public record ReporteReservasPorCanalResponse(
        LocalDate desde,
        LocalDate hasta,
        int totalReservas,
        List<Canal> canales
) {

    /** activas = PENDIENTE, CONFIRMADA o EN_CURSO; porcentaje es la participación del canal, con dos decimales. */
    public record Canal(CanalOrigen canal, int total, int activas, int finalizadas, int canceladas, int noShow,
                        BigDecimal porcentaje) {
    }
}
