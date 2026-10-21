package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Reserva tal como la ve el cliente. Respuesta de CU-03, CU-10 y CU-18.
 * Los objetos de valor se aplanan: CodigoReserva sale como "RES-2026-00001", Dinero como pesos enteros
 * y Estancia como dos fechas más el número de noches.
 */
public record ReservaDetalleResponse(
        String codigo,
        EstadoReserva estado,
        CanalOrigen canalOrigen,
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        int noches,
        LocalTime horaEstimadaLlegada,       // null hasta que se registre (RN-09)
        TitularResponse titular,
        List<OcupanteResponse> ocupantes,
        int totalOcupantes,
        long valorTotal,                      // valor congelado (RN-22), en pesos
        int versionPoliticaCancelacion,       // la versión congelada en la reserva (RN-22)
        LocalDateTime fechaCreacion,
        LocalDateTime fechaConfirmacion,      // null mientras esté PENDIENTE
        String motivoCancelacion              // null si no se ha cancelado
) {
}
