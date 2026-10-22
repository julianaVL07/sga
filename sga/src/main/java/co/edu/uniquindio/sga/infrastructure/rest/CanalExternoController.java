package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.ReservaExternaRequest;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.application.dto.response.EventoCanalResponse;
import co.edu.uniquindio.sga.application.usecase.RecibirReservaExternaUseCase;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService.DatosReservaExterna;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador REST del canal externo (Guía 08): las tres operaciones de CU-08 (10.3.1).
 * {canal} es el nombre de la plataforma que llama (por ejemplo "booking"); para el dominio todas son el canal
 * EXTERNO. Un rechazo por conflicto (RN-18) o un mensaje repetido (RN-19) no es un error HTTP: el canal recibe
 * 200 con el resultado en el cuerpo, y el mensaje queda en la bitácora (7.8).
 */
@RestController
@RequestMapping("/api/canales/{canal}")
@RequiredArgsConstructor
@Tag(name = "Canal externo", description = "Integración con plataformas de terceros (CU-08)")
public class CanalExternoController {

    private final RecibirReservaExternaUseCase recibirReservaExternaUseCase;
    private final ReservaMapper reservaMapper;
    private final ApartamentoMapper apartamentoMapper;

    /** CU-08, operación 1. GET /api/canales/{canal}/disponibilidad?entrada=&salida=&ocupantes= */
    @GetMapping("/disponibilidad")
    @Operation(summary = "Consultar disponibilidad desde el canal",
            description = "Apartamentos libres para la estancia y el grupo, con las mismas reglas que CU-01")
    @ApiResponse(responseCode = "200", description = "Apartamentos disponibles (puede ser una lista vacía)")
    @ApiResponse(responseCode = "400", description = "Falta un parámetro o una fecha está mal escrita")
    @ApiResponse(responseCode = "409", description = "Las fechas no forman una estancia válida")
    public ResponseEntity<List<ApartamentoResponse>> consultarDisponibilidad(@PathVariable String canal,
                                                                             @RequestParam LocalDate entrada,
                                                                             @RequestParam LocalDate salida,
                                                                             @RequestParam int ocupantes) {
        return ResponseEntity.ok(apartamentoMapper.aResponses(
                recibirReservaExternaUseCase.consultarDisponibilidad(entrada, salida, ocupantes)));
    }

    /** CU-08, operación 2. POST /api/canales/{canal}/reservas */
    @PostMapping("/reservas")
    @Operation(summary = "Recibir una reserva del canal",
            description = "Si las noches están libres crea la reserva PENDIENTE con su folio (incluye CU-03). "
                    + "Si ya están vendidas o bloqueadas responde RECHAZADO_CONFLICTO (RN-18); si el "
                    + "identificador externo ya llegó, DUPLICADO_IGNORADO (RN-19)")
    @ApiResponse(responseCode = "200", description = "Mensaje procesado; el resultado va en el cuerpo")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    public ResponseEntity<EventoCanalResponse> recibirReserva(@PathVariable String canal,
                                                              @Valid @RequestBody ReservaExternaRequest request) {
        DatosReservaExterna datos = new DatosReservaExterna(
                reservaMapper.aIdentificacion(request.identificacionApartamento()),
                new Estancia(request.fechaEntrada(), request.fechaSalida()),
                reservaMapper.aTitular(request.titular()),
                reservaMapper.aOcupantes(request.ocupantes()),
                request.horaEstimadaLlegada());
        ResultadoEventoCanal resultado = recibirReservaExternaUseCase.ejecutar(request.identificadorExterno(), datos);
        return ResponseEntity.ok(new EventoCanalResponse(request.identificadorExterno(),
                TipoEventoCanal.CREACION_RESERVA, resultado));
    }

    /** CU-08, operación 3. PUT /api/canales/{canal}/reservas/{identificadorExterno}/cancelar */
    @PutMapping("/reservas/{identificadorExterno}/cancelar")
    @Operation(summary = "Cancelar una reserva del canal",
            description = "Cancela la reserva que el mismo canal creó con ese identificador externo; una "
                    + "cancelación repetida responde DUPLICADO_IGNORADO (RN-19)")
    @ApiResponse(responseCode = "200", description = "Mensaje procesado; el resultado va en el cuerpo")
    public ResponseEntity<EventoCanalResponse> cancelarReserva(@PathVariable String canal,
                                                               @PathVariable String identificadorExterno) {
        ResultadoEventoCanal resultado = recibirReservaExternaUseCase.cancelar(identificadorExterno);
        return ResponseEntity.ok(new EventoCanalResponse(identificadorExterno,
                TipoEventoCanal.CANCELACION_RESERVA, resultado));
    }
}