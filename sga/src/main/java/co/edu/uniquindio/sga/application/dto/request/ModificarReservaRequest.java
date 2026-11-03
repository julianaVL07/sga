package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.List;

/**
 * CU-11. PUT /api/reservas/{codigo}
 * Todos los campos son opcionales: el que viene en null se conserva. Para cambiar solo las fechas,
 * identificacionApartamento y ocupantes van en null. El dominio revalida las condiciones de creación con las
 * tarifas vigentes y asienta la diferencia en el folio (RN-14, RN-22). Las mascotas se revalidan con la política
 * del alojamiento y, si la reserva no las traía, se asienta el cargo de aseo (L-16).
 */
public record ModificarReservaRequest(
        String identificacionApartamento,

        LocalDate fechaEntrada,

        LocalDate fechaSalida,

        List<@Valid OcupanteRequest> ocupantes,

        @Min(value = 0, message = "La cantidad de mascotas no puede ser negativa")
        Integer cantidadMascotas,

        @Min(value = 0, message = "El peso de la mascota no puede ser negativo")
        Integer pesoMascotaMasPesadaKg
) {
}
