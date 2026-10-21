package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * CU-11. PUT /api/reservas/{codigo}
 * Todos los campos son opcionales: el que viene en null se conserva. Para cambiar solo las fechas,
 * identificacionApartamento y ocupantes van en null. El dominio revalida las condiciones de creación con las
 * tarifas vigentes y asienta la diferencia en el folio (RN-14, RN-22).
 */
public record ModificarReservaRequest(
        String identificacionApartamento,

        LocalDate fechaEntrada,

        LocalDate fechaSalida,

        List<@Valid OcupanteRequest> ocupantes
) {
}
