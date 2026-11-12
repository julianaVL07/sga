package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.response.ReporteIngresosResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteOcupacionResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteReservasPorCanalResponse;
import co.edu.uniquindio.sga.application.usecase.ConsultarReportesUseCase;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReporteMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Adaptador REST de los reportes del administrador (7.9). Todos reciben desde y hasta, ambas incluidas.
 * No tiene reglas: las aplica ReportesService y GlobalExceptionHandler traduce los errores (400, 409).
 */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Ocupación, ingresos y reservas por canal de un periodo")
public class ReporteController {

    private final ConsultarReportesUseCase consultarReportesUseCase;
    private final ReporteMapper mapper;

    /** 7.9. GET /api/reportes/ocupacion?desde=&hasta= */
    @GetMapping("/ocupacion")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR')")
    @Operation(summary = "Reporte de ocupación",
            description = "Noches vendidas (reservas CONFIRMADA, EN_CURSO o FINALIZADA) dentro del periodo y "
                    + "porcentaje = noches vendidas / (apartamentos activos × noches del periodo), con el detalle "
                    + "por apartamento (7.9)")
    @ApiResponse(responseCode = "200", description = "Reporte de ocupación del periodo")
    @ApiResponse(responseCode = "400", description = "Falta una fecha o está mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "desde es posterior a hasta (7.9)")
    public ResponseEntity<ReporteOcupacionResponse> ocupacion(@RequestParam LocalDate desde,
                                                              @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(mapper.aResponse(consultarReportesUseCase.ocupacion(desde, hasta)));
    }

    /** 7.9. GET /api/reportes/ingresos?desde=&hasta= */
    @GetMapping("/ingresos")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR')")
    @Operation(summary = "Reporte de ingresos",
            description = "Suma de los pagos registrados en los folios dentro del periodo, por medio de pago. Los "
                    + "cargos no cuentan (7.9)")
    @ApiResponse(responseCode = "200", description = "Reporte de ingresos del periodo")
    @ApiResponse(responseCode = "400", description = "Falta una fecha o está mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "desde es posterior a hasta (7.9)")
    public ResponseEntity<ReporteIngresosResponse> ingresos(@RequestParam LocalDate desde,
                                                            @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(mapper.aResponse(consultarReportesUseCase.ingresos(desde, hasta)));
    }

    /** 7.9. GET /api/reportes/reservas-por-canal?desde=&hasta= */
    @GetMapping("/reservas-por-canal")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR')")
    @Operation(summary = "Reporte de reservas por canal",
            description = "Reservas con fecha de entrada en el periodo por canal de origen, en cualquier estado: "
                    + "incluye las canceladas y los no-show (7.9)")
    @ApiResponse(responseCode = "200", description = "Reporte de reservas por canal del periodo")
    @ApiResponse(responseCode = "400", description = "Falta una fecha o está mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "desde es posterior a hasta (7.9)")
    public ResponseEntity<ReporteReservasPorCanalResponse> reservasPorCanal(@RequestParam LocalDate desde,
                                                                            @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(mapper.aResponse(consultarReportesUseCase.reservasPorCanal(desde, hasta)));
    }
}
