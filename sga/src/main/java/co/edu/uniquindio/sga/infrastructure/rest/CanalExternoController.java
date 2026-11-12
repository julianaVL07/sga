package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.ReservaExternaRequest;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.application.dto.response.EventoCanalResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.RecibirReservaExternaUseCase;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService.DatosReservaExterna;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.PaginaMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import co.edu.uniquindio.sga.infrastructure.security.CanalCredencialFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Adaptador REST del canal externo (Guía 08): las tres operaciones de CU-08 (10.3.1).
 * {canal} es el nombre de la plataforma que llama (por ejemplo "booking"); para el dominio todas son el canal
 * EXTERNO. No usan el JWT: el canal se autentica con su credencial en el encabezado X-Canal-Credencial, que
 * CanalCredencialFilter verifica antes de llegar aquí (401 si falta o no corresponde, 403 si el canal está
 * inactivo). Todo mensaje queda en la bitácora (7.8), también los rechazados: por eso el caso de uso devuelve el
 * resultado en vez de lanzar una excepción, y este controlador lo traduce al código HTTP (estadoHttp).
 */
@RestController
@RequestMapping("/api/canales/{canal}")
@RequiredArgsConstructor
@Tag(name = "Canal externo", description = "Integración con plataformas de terceros (CU-08)")
@SecurityRequirement(name = "credencialCanal")
public class CanalExternoController {

    private final RecibirReservaExternaUseCase recibirReservaExternaUseCase;
    private final ReservaMapper reservaMapper;
    private final ApartamentoMapper apartamentoMapper;

    /** CU-08, operación 1. GET /api/canales/{canal}/disponibilidad?entrada=&salida=&ocupantes=&pagina= */
    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAuthority('" + CanalCredencialFilter.AUTORIDAD_CANAL + "')")
    @Operation(summary = "Consultar disponibilidad desde el canal",
            description = "Apartamentos libres para la estancia y el grupo, con las mismas reglas que CU-01, 10 por "
                    + "página (12.4). La consulta queda en la bitácora (7.8)")
    @ApiResponse(responseCode = "200", description = "Página de apartamentos disponibles (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Falta un parámetro, una fecha está mal escrita o la página es "
            + "negativa")
    @ApiResponse(responseCode = "401", description = "Sin credencial del canal o credencial incorrecta")
    @ApiResponse(responseCode = "403", description = "El canal está inactivo")
    @ApiResponse(responseCode = "409", description = "Las fechas no forman una estancia válida")
    public ResponseEntity<PaginaResponse<ApartamentoResponse>> consultarDisponibilidad(
            @PathVariable String canal,
            @RequestParam LocalDate entrada,
            @RequestParam LocalDate salida,
            @RequestParam int ocupantes,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(PaginaMapper.aResponse(
                recibirReservaExternaUseCase.consultarDisponibilidad(canal, entrada, salida, ocupantes, pagina),
                apartamentoMapper::aResponse));
    }

    /** CU-08, operación 2. POST /api/canales/{canal}/reservas */
    @PostMapping("/reservas")
    @PreAuthorize("hasAuthority('" + CanalCredencialFilter.AUTORIDAD_CANAL + "')")
    @Operation(summary = "Recibir una reserva del canal",
            description = "Si las noches están libres crea la reserva PENDIENTE con su folio (incluye CU-03). "
                    + "Si ya están vendidas o bloqueadas responde RECHAZADO_CONFLICTO (RN-18); si el "
                    + "identificador externo ya llegó, DUPLICADO_IGNORADO (RN-19)")
    @ApiResponse(responseCode = "201", description = "ACEPTADO: reserva creada PENDIENTE")
    @ApiResponse(responseCode = "200", description = "DUPLICADO_IGNORADO: el mensaje ya había llegado (RN-19)")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "Sin credencial del canal o credencial incorrecta")
    @ApiResponse(responseCode = "403", description = "El canal está inactivo")
    @ApiResponse(responseCode = "409", description = "RECHAZADO_CONFLICTO: noches vendidas o bloqueadas (RN-18)")
    @ApiResponse(responseCode = "422", description = "RECHAZADO_INVALIDO: los datos no cumplen las reglas de la reserva")
    public ResponseEntity<EventoCanalResponse> recibirReserva(@PathVariable String canal,
                                                              @Valid @RequestBody ReservaExternaRequest request) {
        DatosReservaExterna datos = new DatosReservaExterna(
                reservaMapper.aIdentificacion(request.identificacionApartamento()),
                new Estancia(request.fechaEntrada(), request.fechaSalida()),
                reservaMapper.aTitular(request.titular()),
                reservaMapper.aOcupantes(request.ocupantes()),
                request.horaEstimadaLlegada());
        ResultadoEventoCanal resultado = recibirReservaExternaUseCase.ejecutar(canal, request.identificadorExterno(),
                datos);
        return ResponseEntity.status(estadoHttp(resultado, HttpStatus.CREATED))
                .body(new EventoCanalResponse(request.identificadorExterno(), TipoEventoCanal.CREACION_RESERVA, resultado));
    }

    /** CU-08, operación 3. PUT /api/canales/{canal}/reservas/{identificadorExterno}/cancelar */
    @PutMapping("/reservas/{identificadorExterno}/cancelar")
    @PreAuthorize("hasAuthority('" + CanalCredencialFilter.AUTORIDAD_CANAL + "')")
    @Operation(summary = "Cancelar una reserva del canal",
            description = "Cancela la reserva que el mismo canal creó con ese identificador externo; una "
                    + "cancelación repetida responde DUPLICADO_IGNORADO (RN-19)")
    @ApiResponse(responseCode = "200", description = "ACEPTADO o DUPLICADO_IGNORADO (RN-19)")
    @ApiResponse(responseCode = "401", description = "Sin credencial del canal o credencial incorrecta")
    @ApiResponse(responseCode = "403", description = "El canal está inactivo")
    @ApiResponse(responseCode = "422", description = "RECHAZADO_INVALIDO: no hay una reserva del canal con ese identificador "
            + "o ya no se puede cancelar")
    public ResponseEntity<EventoCanalResponse> cancelarReserva(@PathVariable String canal,
                                                               @PathVariable String identificadorExterno) {
        ResultadoEventoCanal resultado = recibirReservaExternaUseCase.cancelar(canal, identificadorExterno);
        return ResponseEntity.status(estadoHttp(resultado, HttpStatus.OK))
                .body(new EventoCanalResponse(identificadorExterno, TipoEventoCanal.CANCELACION_RESERVA, resultado));
    }

    /**
     * El caso de uso no lanza excepción en un rechazo para que el evento quede en la bitácora (7.8); aquí solo se
     * traduce el resultado al código HTTP que corresponde. Los rechazos de acceso los responde el filtro antes de
     * llegar aquí; se mapean igual para que el switch cubra todos los resultados.
     */
    private HttpStatus estadoHttp(ResultadoEventoCanal resultado, HttpStatus siAceptado) {
        return switch (resultado) {
            case ACEPTADO -> siAceptado;
            case DUPLICADO_IGNORADO -> HttpStatus.OK;                        // RN-19: idempotente, el canal no reintenta
            case RECHAZADO_CONFLICTO -> HttpStatus.CONFLICT;                 // RN-18
            case RECHAZADO_INVALIDO -> HttpStatus.UNPROCESSABLE_CONTENT;
            case RECHAZADO_CREDENCIAL -> HttpStatus.UNAUTHORIZED;
            case RECHAZADO_CANAL_INACTIVO -> HttpStatus.FORBIDDEN;
        };
    }
}
