package co.edu.uniquindio.sga.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/reservas/{codigo}/folio/cargos (7.1: registrar cargos en el folio)
 * El servicio adicional que se carga al folio, por ejemplo SRV-CENA-BALCON. El valor y el concepto los pone el
 * servicio; la fecha, el reloj del SGA. Que el servicio exista, esté activo y genere cargo, y que el folio esté
 * abierto, son reglas del dominio (409).
 */
public record RegistrarCargoServicioRequest(
        @NotBlank(message = "El id del servicio adicional es obligatorio")
        String idServicio
) {
}
