package co.edu.uniquindio.sga.application.dto.response;

import java.util.List;

/**
 * CU-23 (7.3): el apartamento actualizado y la advertencia de capacidad. reservasSobreCapacidad lista los códigos
 * de las reservas activas o futuras cuyo grupo supera la nueva capacidad; esas reservas no se modifican.
 * advertencia es null si no hay reservas afectadas.
 */
public record ActualizacionApartamentoResponse(
        ApartamentoDetalleResponse apartamento,
        List<String> reservasSobreCapacidad,
        String advertencia
) {
}
