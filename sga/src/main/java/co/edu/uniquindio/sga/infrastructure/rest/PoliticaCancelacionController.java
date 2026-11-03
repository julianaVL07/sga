package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.ActualizarPoliticaCancelacionRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.PoliticaCancelacionResponse;
import co.edu.uniquindio.sga.application.usecase.PoliticaCancelacionUseCase;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ConfiguracionMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.PaginaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador REST de la política de cancelación versionada (Guía 08): CU-30. No tiene reglas ni try/catch: los
 * errores los traduce GlobalExceptionHandler (400, 409).
 */
@RestController
@RequestMapping("/api/politica-cancelacion")
@RequiredArgsConstructor
@Tag(name = "Configuración del alojamiento", description = "Alojamiento, temporadas y política de cancelación")
public class PoliticaCancelacionController {

    private final PoliticaCancelacionUseCase politicaCancelacionUseCase;
    private final ConfiguracionMapper mapper;

    /** CU-30. GET /api/politica-cancelacion */
    @GetMapping
    @Operation(summary = "Consultar la política de cancelación vigente",
            description = "La única versión vigente (7.4): la que se congela en cada reserva nueva (RN-22)")
    @ApiResponse(responseCode = "200", description = "La versión vigente")
    public ResponseEntity<PoliticaCancelacionResponse> vigente() {
        return ResponseEntity.ok(mapper.aPoliticaResponse(politicaCancelacionUseCase.vigente()));
    }

    /** CU-30. GET /api/politica-cancelacion/versiones?pagina= */
    @GetMapping("/versiones")
    @Operation(summary = "Listar las versiones de la política de cancelación",
            description = "Todas las versiones, de la más reciente a la más antigua, 10 por página (F-07, 12.4)")
    @ApiResponse(responseCode = "200", description = "Página de versiones")
    @ApiResponse(responseCode = "400", description = "Número de página inválido")
    public ResponseEntity<PaginaResponse<PoliticaCancelacionResponse>> versiones(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        Pagina<PoliticaCancelacion> versiones = politicaCancelacionUseCase.versiones(pagina);
        return ResponseEntity.ok(PaginaMapper.aResponse(versiones, mapper::aPoliticaResponse));
    }

    /** CU-30. PUT /api/politica-cancelacion */
    @PutMapping
    @Operation(summary = "Actualizar la política de cancelación",
            description = "Crea la versión N+1 con los tramos y la consecuencia del no-show dados, vigente desde hoy. "
                    + "La versión anterior se conserva y las reservas existentes siguen con la que congelaron (RN-22)")
    @ApiResponse(responseCode = "200", description = "Nueva versión vigente")
    @ApiResponse(responseCode = "400", description = "Falta la consecuencia del no-show, los tramos o un dato de un "
            + "tramo")
    @ApiResponse(responseCode = "409", description = "Menos de dos tramos, antelaciones repetidas, sin tramo desde 0 "
            + "días o porcentaje fuera de 0 a 100 (L-10)")
    public ResponseEntity<PoliticaCancelacionResponse> actualizar(
            @Valid @RequestBody ActualizarPoliticaCancelacionRequest request) {
        PoliticaCancelacion nueva = politicaCancelacionUseCase.actualizar(request.consecuenciaNoShow(),
                mapper.aTramos(request.tramos()));
        return ResponseEntity.ok(mapper.aPoliticaResponse(nueva));
    }
}
