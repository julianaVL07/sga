package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.response.ReservaDetalleResponse;
import co.edu.uniquindio.sga.application.usecase.ConsultarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarLlegadaUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarSalidaUseCase;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador REST de la operación en recepción sobre una reserva (Guía 08): CU-04 y CU-05.
 * Comparte la ruta /api/reservas con ReservaController, pero va en su propia clase porque cambia también el
 * estado del apartamento. Responde con la reserva actualizada.
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Operación en recepción", description = "Registro de llegada y salida del grupo")
public class OperacionReservaController {

    private final RegistrarLlegadaUseCase registrarLlegadaUseCase;
    private final RegistrarSalidaUseCase registrarSalidaUseCase;
    private final ConsultarReservaUseCase consultarReservaUseCase;
    private final ReservaMapper reservaMapper;

    /** CU-04. PUT /api/reservas/{codigo}/registrar-llegada */
    @PutMapping("/{codigo}/registrar-llegada")
    @Operation(summary = "Registrar la llegada",
            description = "La reserva pasa a EN_CURSO y el apartamento a OCUPADO. Exige reserva CONFIRMADA con la "
                    + "fecha de entrada alcanzada (RN-10) y apartamento PREPARADO (RN-11)")
    @ApiResponse(responseCode = "200", description = "Llegada registrada")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "La reserva no está CONFIRMADA, no es su fecha o el apartamento no está PREPARADO")
    public ResponseEntity<ReservaDetalleResponse> registrarLlegada(@PathVariable String codigo) {
        CodigoReserva codigoReserva = new CodigoReserva(codigo);
        registrarLlegadaUseCase.ejecutar(codigoReserva);
        return ResponseEntity.ok(reservaMapper.aDetalle(consultarReservaUseCase.ejecutar(codigoReserva)));
    }

    /** CU-05. PUT /api/reservas/{codigo}/registrar-salida */
    @PutMapping("/{codigo}/registrar-salida")
    @Operation(summary = "Registrar la salida",
            description = "La reserva pasa a FINALIZADA (RN-12) y el apartamento a PENDIENTE_PREPARACION. El folio "
                    + "debe quedar cerrado: saldo en cero o autorización del administrador (RN-17)")
    @ApiResponse(responseCode = "200", description = "Salida registrada")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "La reserva no está EN_CURSO o el folio tiene saldo sin autorizar")
    public ResponseEntity<ReservaDetalleResponse> registrarSalida(@PathVariable String codigo) {
        CodigoReserva codigoReserva = new CodigoReserva(codigo);
        registrarSalidaUseCase.ejecutar(codigoReserva);
        return ResponseEntity.ok(reservaMapper.aDetalle(consultarReservaUseCase.ejecutar(codigoReserva)));
    }
}