package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.CambiarEstadoOperativoRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarBloqueoRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarNovedadRequest;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.application.dto.response.BloqueoResponse;
import co.edu.uniquindio.sga.application.dto.response.NovedadResponse;
import co.edu.uniquindio.sga.application.usecase.BuscarApartamentoDisponibleUseCase;
import co.edu.uniquindio.sga.application.usecase.CambiarEstadoOperativoUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarBloqueoUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarNovedadUseCase;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * Adaptador REST de apartamentos y operación (Guía 08): CU-01, CU-13, CU-16 y CU-17.
 * {id} es la identificación del apartamento, por ejemplo APT-101. No tiene reglas: las aplica el dominio
 * y GlobalExceptionHandler traduce los errores (400, 404, 409).
 */
@RestController
@RequestMapping("/api/apartamentos")
@RequiredArgsConstructor
@Tag(name = "Apartamentos", description = "Disponibilidad y operación de los apartamentos del Apartahotel Coral")
public class ApartamentoController {

    private final BuscarApartamentoDisponibleUseCase buscarApartamentoDisponibleUseCase;
    private final RegistrarBloqueoUseCase registrarBloqueoUseCase;
    private final CambiarEstadoOperativoUseCase cambiarEstadoOperativoUseCase;
    private final RegistrarNovedadUseCase registrarNovedadUseCase;
    private final ApartamentoMapper mapper;

    /** CU-01. GET /api/apartamentos/disponibles?entrada=&salida=&ocupantes= */
    @GetMapping("/disponibles")
    @Operation(summary = "Buscar apartamentos disponibles",
            description = "Apartamentos libres en todas las noches de la estancia y con capacidad para el grupo "
                    + "(RN-01, RN-02, RN-07, RN-20). Solo consulta")
    @ApiResponse(responseCode = "200", description = "Apartamentos disponibles (puede ser una lista vacía)")
    @ApiResponse(responseCode = "400", description = "Falta un parámetro o una fecha está mal escrita")
    @ApiResponse(responseCode = "409", description = "Las fechas no forman una estancia válida (RN-03, RN-04)")
    public ResponseEntity<List<ApartamentoResponse>> buscarDisponibles(@RequestParam LocalDate entrada,
                                                                       @RequestParam LocalDate salida,
                                                                       @RequestParam int ocupantes) {
        List<Apartamento> disponibles = buscarApartamentoDisponibleUseCase.ejecutar(entrada, salida, ocupantes);
        return ResponseEntity.ok(mapper.aResponses(disponibles));
    }

    /** CU-13. POST /api/apartamentos/{id}/bloqueos */
    @PostMapping("/{id}/bloqueos")
    @Operation(summary = "Registrar un bloqueo",
            description = "Retira el apartamento de la venta en un rango de fechas; se rechaza si hay reservas "
                    + "activas en esas noches (7.3, RN-01)")
    @ApiResponse(responseCode = "201", description = "Bloqueo registrado")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "404", description = "No existe un apartamento con esa identificación")
    @ApiResponse(responseCode = "409", description = "Rango inválido o noches con reservas activas")
    public ResponseEntity<BloqueoResponse> registrarBloqueo(@PathVariable String id,
                                                            @Valid @RequestBody RegistrarBloqueoRequest request) {
        Bloqueo bloqueo = registrarBloqueoUseCase.ejecutar(new IdentificacionApartamento(id),
                request.fechaInicio(), request.fechaFin(), request.motivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aBloqueoResponse(bloqueo));
    }

    /** CU-16. PUT /api/apartamentos/{id}/estado-operativo */
    @PutMapping("/{id}/estado-operativo")
    @Operation(summary = "Cambiar el estado operativo",
            description = "Solo por las transiciones permitidas (7.6): PENDIENTE_PREPARACION → EN_PREPARACION → "
                    + "PREPARADO; FUERA_DE_SERVICIO lo decide el administrador")
    @ApiResponse(responseCode = "200", description = "Estado cambiado")
    @ApiResponse(responseCode = "400", description = "Falta el estado o no existe ese valor")
    @ApiResponse(responseCode = "404", description = "No existe un apartamento con esa identificación")
    @ApiResponse(responseCode = "409", description = "Transición de estado no permitida")
    public ResponseEntity<ApartamentoResponse> cambiarEstadoOperativo(
            @PathVariable String id, @Valid @RequestBody CambiarEstadoOperativoRequest request) {
        Apartamento apartamento = cambiarEstadoOperativoUseCase.ejecutar(new IdentificacionApartamento(id),
                request.nuevoEstado());
        return ResponseEntity.ok(mapper.aResponse(apartamento));
    }

    /** CU-17. POST /api/apartamentos/{id}/novedades */
    @PostMapping("/{id}/novedades")
    @Operation(summary = "Registrar una novedad",
            description = "Daño, faltante o incidencia del apartamento con autor, descripción y gravedad (7.6)")
    @ApiResponse(responseCode = "201", description = "Novedad registrada")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "404", description = "No existe un apartamento con esa identificación")
    public ResponseEntity<NovedadResponse> registrarNovedad(@PathVariable String id,
                                                            @Valid @RequestBody RegistrarNovedadRequest request) {
        Novedad novedad = registrarNovedadUseCase.ejecutar(new IdentificacionApartamento(id),
                request.autor(), request.descripcion(), request.gravedad());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aNovedadResponse(novedad));
    }
}