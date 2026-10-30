package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.CancelarReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.ConfirmarReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.ModificarReservaRequest;
import co.edu.uniquindio.sga.application.dto.response.ModificacionReservaResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaDetalleResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaResumenResponse;
import co.edu.uniquindio.sga.application.dto.response.RetencionResponse;
import co.edu.uniquindio.sga.application.usecase.CancelarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConfirmarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.DeclararNoShowUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarReservasUseCase;
import co.edu.uniquindio.sga.application.usecase.ModificarReservaUseCase;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

/**
 * Adaptador REST del agregado Reserva (Guía 08): CU-03, CU-09, CU-10, CU-11, CU-12, CU-18 y CU-19.
 * No tiene reglas ni try/catch: convierte el DTO a tipos del dominio, llama al caso de uso y mapea la respuesta.
 * Los errores los traduce GlobalExceptionHandler (400, 404, 409). La fecha y hora las pone el reloj del SGA,
 * nunca el cliente. El registro de llegada y salida (CU-04, CU-05) está en OperacionReservaController.
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Ciclo de vida del agregado Reserva en el Apartahotel Coral")
public class ReservaController {

    private final CrearReservaUseCase crearReservaUseCase;
    private final ConsultarReservaUseCase consultarReservaUseCase;
    private final ConfirmarReservaUseCase confirmarReservaUseCase;
    private final ModificarReservaUseCase modificarReservaUseCase;
    private final CancelarReservaUseCase cancelarReservaUseCase;
    private final DeclararNoShowUseCase declararNoShowUseCase;
    private final ListarReservasUseCase listarReservasUseCase;
    private final ReservaMapper mapper;

    /** CU-03. POST /api/reservas */
    @PostMapping
    @Operation(summary = "Crear una reserva",
            description = "Crea la reserva en PENDIENTE, congela cotización y política (RN-22) y abre su folio con el "
                    + "depósito de garantía (RP-02) y, si la llegada es desde las 20:00, la recepción nocturna (RP-03)")
    @ApiResponse(responseCode = "201", description = "Reserva creada")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "409", description = "Una regla del negocio lo impide (RN-01 a RN-07, RP-01, RN-19)")
    public ResponseEntity<ReservaDetalleResponse> crear(@Valid @RequestBody CrearReservaRequest request) {
        Reserva reserva = crearReservaUseCase.ejecutar(mapper.aSolicitud(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{codigo}")
                .buildAndExpand(reserva.getCodigo().valor())
                .toUri();
        return ResponseEntity.created(location).body(mapper.aDetalle(reserva));
    }

    /** CU-19. GET /api/reservas?estado=&apartamento=&canal=&titular=&desde=&hasta=&pagina=0 */
    @GetMapping
    @Operation(summary = "Listar reservas",
            description = "Reservas de la más reciente a la más antigua, 10 por página (7.5, 12.4). Todos los filtros "
                    + "son opcionales; desde y hasta acotan la fecha de entrada (ambas incluidas) y titular es el "
                    + "número de documento del titular. La numeración de páginas empieza en 0")
    @ApiResponse(responseCode = "200", description = "Página de reservas (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa, o estado, canal o fecha mal escritos")
    @ApiResponse(responseCode = "409", description = "La fecha desde es posterior a la fecha hasta")
    public ResponseEntity<PaginaResponse<ReservaResumenResponse>> listar(
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) String apartamento,
            @RequestParam(required = false) CanalOrigen canal,
            @RequestParam(required = false) String titular,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        FiltroReservas filtro = new FiltroReservas(estado, mapper.aIdentificacion(apartamento), canal, titular,
                desde, hasta);
        Pagina<Reserva> reservas = listarReservasUseCase.ejecutar(filtro, pagina);
        return ResponseEntity.ok(PaginaResponse.de(reservas, mapper::aResumen));
    }

    /** CU-18. GET /api/reservas/{codigo} */
    @GetMapping("/{codigo}")
    @Operation(summary = "Consultar una reserva")
    @ApiResponse(responseCode = "200", description = "Detalle de la reserva")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "El código no tiene el formato RES-YYYY-NNNNN")
    public ResponseEntity<ReservaDetalleResponse> consultar(@PathVariable String codigo) {
        Reserva reserva = consultarReservaUseCase.ejecutar(new CodigoReserva(codigo));
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** CU-11. PUT /api/reservas/{codigo} */
    @PutMapping("/{codigo}")
    @Operation(summary = "Modificar una reserva",
            description = "Cambia fechas, ocupantes o apartamento de una reserva no iniciada; revalida con las tarifas "
                    + "vigentes y asienta la diferencia en el folio (RN-14). Un campo en null se conserva")
    @ApiResponse(responseCode = "200", description = "Reserva modificada")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "Una regla del negocio lo impide (RN-01 a RN-08, RN-14)")
    public ResponseEntity<ModificacionReservaResponse> modificar(@PathVariable String codigo,
                                                                 @Valid @RequestBody ModificarReservaRequest request) {
        CodigoReserva codigoReserva = new CodigoReserva(codigo);
        Dinero diferencia = modificarReservaUseCase.ejecutar(codigoReserva,
                mapper.aIdentificacion(request.identificacionApartamento()),
                request.fechaEntrada(), request.fechaSalida(),
                request.ocupantes() == null ? null : mapper.aOcupantes(request.ocupantes()));
        Reserva reserva = consultarReservaUseCase.ejecutar(codigoReserva);
        return ResponseEntity.ok(new ModificacionReservaResponse(mapper.aDetalle(reserva), mapper.aPesos(diferencia)));
    }

    /** CU-10. PUT /api/reservas/{codigo}/confirmar */
    @PutMapping("/{codigo}/confirmar")
    @Operation(summary = "Confirmar una reserva",
            description = "PENDIENTE → CONFIRMADA. Exige hora estimada de llegada (RN-09), plazo vigente (RN-21) "
                    + "y anticipo del 50 % cubierto en el folio (L-11). El cuerpo es opcional")
    @ApiResponse(responseCode = "200", description = "Reserva confirmada")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "Transición inválida, falta la hora, venció el plazo o falta anticipo")
    public ResponseEntity<ReservaDetalleResponse> confirmar(@PathVariable String codigo,
                                                            @RequestBody(required = false) ConfirmarReservaRequest request) {
        Reserva reserva = confirmarReservaUseCase.ejecutar(new CodigoReserva(codigo),
                request == null ? null : request.horaEstimadaLlegada());
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** CU-09. PUT /api/reservas/{codigo}/cancelar */
    @PutMapping("/{codigo}/cancelar")
    @Operation(summary = "Cancelar una reserva",
            description = "Calcula la retención con la política congelada (RN-13), la asienta en el folio y libera "
                    + "las noches de inmediato (RN-12)")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada, con la retención aplicada")
    @ApiResponse(responseCode = "400", description = "Falta el motivo")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "La reserva ya inició o está en un estado terminal (RN-08)")
    public ResponseEntity<RetencionResponse> cancelar(@PathVariable String codigo,
                                                      @Valid @RequestBody CancelarReservaRequest request) {
        Dinero retencion = cancelarReservaUseCase.ejecutar(new CodigoReserva(codigo), request.motivo());
        return ResponseEntity.ok(new RetencionResponse(codigo, EstadoReserva.CANCELADA, mapper.aPesos(retencion)));
    }

    /** CU-12. PUT /api/reservas/{codigo}/no-show */
    @PutMapping("/{codigo}/no-show")
    @Operation(summary = "Declarar no-show",
            description = "Desde las 22:00 del día de entrada (L-15) marca NO_SHOW, retiene el 50 % del total "
                    + "(RN-13) y libera las noches (RN-12)")
    @ApiResponse(responseCode = "200", description = "No-show declarado, con la retención aplicada")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "Aún no es la hora límite o la reserva no está CONFIRMADA")
    public ResponseEntity<RetencionResponse> declararNoShow(@PathVariable String codigo) {
        Dinero retencion = declararNoShowUseCase.ejecutar(new CodigoReserva(codigo));
        return ResponseEntity.ok(new RetencionResponse(codigo, EstadoReserva.NO_SHOW, mapper.aPesos(retencion)));
    }
}
