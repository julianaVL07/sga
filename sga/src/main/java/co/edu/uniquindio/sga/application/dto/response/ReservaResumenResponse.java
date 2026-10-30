package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

import java.time.LocalDate;

/**
 * Una fila del listado de reservas (CU-19, 7.5 "Listados"). Solo los datos que se muestran en la tabla;
 * el detalle completo se pide con GET /api/reservas/{codigo} (ReservaDetalleResponse).
 */
public record ReservaResumenResponse(
        String codigo,
        EstadoReserva estado,
        String apartamento,                   // "APT-101"
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        String nombreTitular,
        int totalOcupantes,
        CanalOrigen canalOrigen,
        long valorTotal                       // valor congelado (RN-22), en pesos
) {
}
