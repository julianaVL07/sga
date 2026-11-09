package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.CrearTemporadaRequest;
import co.edu.uniquindio.sga.application.dto.request.RangoFechasRequest;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.TemporadaResponse;
import co.edu.uniquindio.sga.application.usecase.TemporadasUseCase;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ConfiguracionMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.PaginaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Adaptador REST de las temporadas (Guía 08): CU-29. {id} es el id de la temporada, por ejemplo T-ALTA.
 * No tiene reglas ni try/catch: los errores los traduce GlobalExceptionHandler (400, 404, 409).
 */
@RestController
@RequestMapping("/api/temporadas")
@RequiredArgsConstructor
@Tag(name = "Configuración del alojamiento", description = "Alojamiento, temporadas y política de cancelación")
public class TemporadaController {

    private final TemporadasUseCase temporadasUseCase;
    private final ConfiguracionMapper mapper;

    /** CU-29. GET /api/temporadas?pagina= */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO')")
    @Operation(summary = "Listar temporadas",
            description = "Temporadas del alojamiento con sus rangos de fechas (fecha final excluida), en el orden en "
                    + "que se registraron, 10 por página (12.4). La base no tiene rangos (F-05)")
    @ApiResponse(responseCode = "200", description = "Página de temporadas")
    @ApiResponse(responseCode = "400", description = "Número de página inválido")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    public ResponseEntity<PaginaResponse<TemporadaResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        Pagina<Temporada> temporadas = temporadasUseCase.listar(pagina);
        return ResponseEntity.ok(PaginaMapper.aResponse(temporadas, mapper::aTemporadaResponse));
    }

    /** CU-29. GET /api/temporadas/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR', 'PERSONAL_SERVICIO')")
    @Operation(summary = "Consultar una temporada",
            description = "La temporada con sus rangos de fechas (fecha final excluida). Es el recurso al que apunta "
                    + "la cabecera Location al crearla o al agregarle un rango")
    @ApiResponse(responseCode = "200", description = "La temporada")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "No existe la temporada")
    public ResponseEntity<TemporadaResponse> consultar(@PathVariable String id) {
        return ResponseEntity.ok(mapper.aTemporadaResponse(temporadasUseCase.consultar(id)));
    }

    /** CU-29. POST /api/temporadas */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR')")
    @Operation(summary = "Crear una temporada",
            description = "Crea la temporada con sus rangos y asigna en la misma operación la tarifa de cada "
                    + "apartamento activo, que queda en su histórico desde hoy (7.4, CU-24)")
    @ApiResponse(responseCode = "201", description = "Temporada creada; Location apunta a la temporada")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o tipo de temporada inexistente")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "Falta la tarifa de un apartamento activo, la temporada se "
            + "solapa con otra o ya hay una temporada base (7.4, F-05)")
    public ResponseEntity<TemporadaResponse> crear(@Valid @RequestBody CrearTemporadaRequest request) {
        Temporada temporada = temporadasUseCase.crear(mapper.aSolicitud(request));
        return ResponseEntity.created(ubicacion(temporada)).body(mapper.aTemporadaResponse(temporada));
    }

    /** CU-29. POST /api/temporadas/{id}/rangos */
    @PostMapping("/{id}/rangos")
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR')")
    @Operation(summary = "Agregar un rango a una temporada",
            description = "El rango (fecha final excluida) no puede solaparse con otra temporada ni con los rangos "
                    + "de la misma; la temporada base no tiene rangos (7.4, F-05)")
    @ApiResponse(responseCode = "201", description = "Rango agregado; devuelve la temporada con todos sus rangos y "
            + "Location apunta a la temporada")
    @ApiResponse(responseCode = "400", description = "Falta una fecha o está mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "No existe la temporada")
    @ApiResponse(responseCode = "409", description = "El rango se solapa, está invertido o la temporada es la base "
            + "(7.4, F-05)")
    public ResponseEntity<TemporadaResponse> agregarRango(@PathVariable String id,
                                                          @Valid @RequestBody RangoFechasRequest request) {
        Temporada temporada = temporadasUseCase.agregarRango(id, mapper.aRango(request));
        return ResponseEntity.created(ubicacion(temporada)).body(mapper.aTemporadaResponse(temporada));
    }

    /** Location de una temporada: /api/temporadas/{id}, que atiende consultar. */
    private static URI ubicacion(Temporada temporada) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/temporadas/{id}")
                .buildAndExpand(temporada.getId())
                .toUri();
    }
}
