package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.CambiarEstadoOperativoRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarBloqueoRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarNovedadRequest;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoDetalleResponse;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoDisponibleResponse;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.application.dto.response.BloqueoResponse;
import co.edu.uniquindio.sga.application.dto.response.NovedadResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.BuscarApartamentoDisponibleUseCase;
import co.edu.uniquindio.sga.application.usecase.CambiarEstadoOperativoUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarApartamentosUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarBloqueoUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarNovedadUseCase;
import co.edu.uniquindio.sga.application.usecase.RetirarApartamentoUseCase;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.ApartamentoDisponible;
import co.edu.uniquindio.sga.domain.valueobject.CriterioBusqueda;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
 * Adaptador REST de apartamentos y operación (Guía 08): CU-01, CU-13, CU-16, CU-17, CU-20 y CU-21.
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
    private final ConsultarApartamentosUseCase consultarApartamentosUseCase;
    private final RetirarApartamentoUseCase retirarApartamentoUseCase;
    private final ApartamentoMapper mapper;

    /** CU-01. GET /api/apartamentos/disponibles?entrada=&salida=&ocupantes=&...&pagina= */
    @GetMapping("/disponibles")
    @Operation(summary = "Buscar apartamentos disponibles",
            description = "Apartamentos libres en todas las noches de la estancia y con capacidad para el grupo "
                    + "(RN-01, RN-02, RN-07, RN-20), filtrados por capacidad mínima, característica de la dotación "
                    + "y rango de precio del valor estimado de la estancia (7.5). 10 resultados por página")
    @ApiResponse(responseCode = "200", description = "Página de apartamentos disponibles (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Falta un parámetro o una fecha está mal escrita")
    @ApiResponse(responseCode = "409", description = "Estancia o filtros inválidos (RN-03, RN-04, RP-01, 7.5)")
    public ResponseEntity<PaginaResponse<ApartamentoDisponibleResponse>> buscarDisponibles(
            @RequestParam LocalDate entrada,
            @RequestParam LocalDate salida,
            @RequestParam int ocupantes,
            @RequestParam(required = false) List<LocalDate> fechasNacimiento,
            @RequestParam(required = false) Integer capacidadMinima,
            @RequestParam(required = false) Long precioMinimo,
            @RequestParam(required = false) Long precioMaximo,
            @RequestParam(required = false) String caracteristica,
            @RequestParam(defaultValue = "0") @Min(0) int pagina) {
        CriterioBusqueda criterio = new CriterioBusqueda(capacidadMinima,
                precioMinimo == null ? null : Dinero.de(precioMinimo),
                precioMaximo == null ? null : Dinero.de(precioMaximo),
                caracteristica);
        Pagina<ApartamentoDisponible> disponibles = buscarApartamentoDisponibleUseCase.buscar(entrada, salida,
                ocupantes, fechasNacimiento, criterio, pagina);
        return ResponseEntity.ok(PaginaResponse.de(disponibles, mapper::aDisponibleResponse));
    }

    /** CU-20. GET /api/apartamentos?pagina= */
    @GetMapping
    @Operation(summary = "Listar apartamentos",
            description = "Apartamentos no eliminados del alojamiento, ordenados por identificación, 10 por página "
                    + "(7.3, 12.4)")
    @ApiResponse(responseCode = "200", description = "Página de apartamentos (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Número de página inválido")
    public ResponseEntity<PaginaResponse<ApartamentoResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int pagina) {
        Pagina<Apartamento> apartamentos = consultarApartamentosUseCase.listar(pagina);
        return ResponseEntity.ok(PaginaResponse.de(apartamentos, mapper::aResponse));
    }

    /** CU-20. GET /api/apartamentos/{id} */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar el detalle de un apartamento",
            description = "Datos, dotación, imágenes, capacidad, estado operativo, tarifas por temporada y bloqueos "
                    + "vigentes (7.3)")
    @ApiResponse(responseCode = "200", description = "Detalle del apartamento")
    @ApiResponse(responseCode = "404", description = "No existe o fue retirado de la venta")
    public ResponseEntity<ApartamentoDetalleResponse> detalle(@PathVariable String id) {
        Apartamento apartamento = consultarApartamentosUseCase.detalle(new IdentificacionApartamento(id));
        return ResponseEntity.ok(mapper.aDetalleResponse(apartamento));
    }

    /** CU-21. PUT /api/apartamentos/{id}/retirar. No hay DELETE: la eliminación es lógica (12.4). */
    @PutMapping("/{id}/retirar")
    @Operation(summary = "Retirar un apartamento de la venta",
            description = "Eliminación lógica: solo si no tiene reservas activas ni futuras (7.3, 12.4)")
    @ApiResponse(responseCode = "200", description = "Apartamento retirado de la venta")
    @ApiResponse(responseCode = "404", description = "No existe un apartamento con esa identificación")
    @ApiResponse(responseCode = "409", description = "Tiene reservas activas o futuras, o ya estaba retirado")
    public ResponseEntity<ApartamentoDetalleResponse> retirar(@PathVariable String id) {
        Apartamento apartamento = retirarApartamentoUseCase.ejecutar(new IdentificacionApartamento(id));
        return ResponseEntity.ok(mapper.aDetalleResponse(apartamento));
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