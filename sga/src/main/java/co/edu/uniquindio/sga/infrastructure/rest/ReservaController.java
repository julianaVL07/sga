package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.CancelarReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.ConfirmarReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.ModificarReservaRequest;
import co.edu.uniquindio.sga.application.dto.response.CalendarioOcupacionResponse;
import co.edu.uniquindio.sga.application.dto.response.ModificacionReservaResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaDetalleResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaResumenResponse;
import co.edu.uniquindio.sga.application.dto.response.RetencionResponse;
import co.edu.uniquindio.sga.application.usecase.CancelarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConfirmarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarCalendarioOcupacionUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.DeclararNoShowUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarLlegadasYSalidasUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarReservasUseCase;
import co.edu.uniquindio.sga.application.usecase.ModificarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarLlegadaUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarSalidaUseCase;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.OcupacionMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.PaginaMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import java.util.List;

/**
 * Adaptador REST del recurso /api/reservas (Guía 08): CU-03, CU-04, CU-05, CU-09, CU-10, CU-11, CU-12, CU-18,
 * CU-19, CU-26 y CU-27. No tiene reglas ni try/catch: convierte el DTO a tipos del dominio, llama al caso de uso y
 * mapea la respuesta. Los errores los traduce GlobalExceptionHandler (400, 404, 409). La fecha y hora las pone el
 * reloj del SGA, nunca el cliente. El registro de llegada y salida y el calendario conservan el tag
 * "Operación en recepción" del contrato; el folio es un subrecurso con su propio controlador (FolioController).
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    // Cada operación declara su tag: springdoc sumaría un @Tag de la clase al de las operaciones de recepción
    private static final String TAG_RESERVAS = "Reservas";
    private static final String TAG_RECEPCION = "Operación en recepción";

    private final CrearReservaUseCase crearReservaUseCase;
    private final ConsultarReservaUseCase consultarReservaUseCase;
    private final ConfirmarReservaUseCase confirmarReservaUseCase;
    private final ModificarReservaUseCase modificarReservaUseCase;
    private final CancelarReservaUseCase cancelarReservaUseCase;
    private final DeclararNoShowUseCase declararNoShowUseCase;
    private final ListarReservasUseCase listarReservasUseCase;
    private final ListarLlegadasYSalidasUseCase listarLlegadasYSalidasUseCase;
    private final RegistrarLlegadaUseCase registrarLlegadaUseCase;
    private final RegistrarSalidaUseCase registrarSalidaUseCase;
    private final ConsultarCalendarioOcupacionUseCase consultarCalendarioUseCase;
    private final ReservaMapper mapper;
    private final OcupacionMapper ocupacionMapper;

    /** CU-03. POST /api/reservas */
    @PostMapping
    // El canal según el rol es regla de acceso, no de negocio: depende de quién inició sesión
    // y el dominio no conoce usuarios (3.6)
    @PreAuthorize("hasAnyAuthority('HUESPED', 'RECEPCIONISTA', 'ADMINISTRADOR') "
            + "and @canalPorRol.puedeCrear(#request, authentication)")
    @Operation(tags = TAG_RESERVAS, summary ="Crear una reserva",
            description = "Crea la reserva en PENDIENTE, congela cotización y política (RN-22) y abre su folio con el "
                    + "depósito de garantía (RP-02), si la llegada es desde las 20:00, la recepción nocturna (RP-03) "
                    + "y, si trae mascotas, el cargo único de aseo (L-16)")
    @ApiResponse(responseCode = "201", description = "Reserva creada")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "Una regla del negocio lo impide (RN-01 a RN-07, RP-01, RN-19, L-16)")
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
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RESERVAS, summary ="Listar reservas",
            description = "Reservas de la más reciente a la más antigua, 10 por página (7.5, 12.4). Todos los filtros "
                    + "son opcionales; desde y hasta acotan la fecha de entrada (ambas incluidas) y titular es el "
                    + "número de documento del titular. La numeración de páginas empieza en 0")
    @ApiResponse(responseCode = "200", description = "Página de reservas (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa, o estado, canal o fecha mal escritos")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
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
        return ResponseEntity.ok(PaginaMapper.aResponse(reservas, mapper::aResumen));
    }

    /** 7.1. GET /api/reservas/mias?estado=&pagina=0: historial del huésped */
    @GetMapping("/mias")
    @PreAuthorize("hasAuthority('HUESPED')")
    @Operation(tags = TAG_RESERVAS, summary ="Listar mis reservas",
            description = "Historial del huésped: solo las reservas cuyo titular tiene el documento del token, 10 por "
                    + "página (12.4). El estado es opcional. La numeración de páginas empieza en 0")
    @ApiResponse(responseCode = "200", description = "Página de reservas del huésped (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa o estado mal escrito")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    public ResponseEntity<PaginaResponse<ReservaResumenResponse>> listarMias(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        String documento = jwt.getClaimAsString("documento");
        // Sin documento, FiltroReservas no filtraría por titular y el huésped vería reservas ajenas
        if (documento == null || documento.isBlank()) {
            throw new AccessDeniedException("El token no trae el documento del usuario");
        }
        FiltroReservas filtro = new FiltroReservas(estado, null, null, documento, null, null);
        Pagina<Reserva> reservas = listarReservasUseCase.ejecutar(filtro, pagina);
        return ResponseEntity.ok(PaginaMapper.aResponse(reservas, mapper::aResumen));
    }

    /** CU-26. GET /api/reservas/llegadas?fecha=&pagina=0 */
    @GetMapping("/llegadas")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RESERVAS, summary ="Llegadas previstas del día",
            description = "Reservas CONFIRMADA cuya fecha de entrada es la fecha pedida (7.1, 7.6), ordenadas por "
                    + "apartamento, 10 por página. Sin fecha se usa hoy en la zona horaria del alojamiento")
    @ApiResponse(responseCode = "200", description = "Página de llegadas (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa o fecha mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    public ResponseEntity<PaginaResponse<ReservaResumenResponse>> llegadas(
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        Pagina<Reserva> reservas = listarLlegadasYSalidasUseCase.llegadas(fecha, pagina);
        return ResponseEntity.ok(PaginaMapper.aResponse(reservas, mapper::aResumen));
    }

    /** CU-26. GET /api/reservas/salidas?fecha=&pagina=0 */
    @GetMapping("/salidas")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RESERVAS, summary ="Salidas previstas del día",
            description = "Reservas EN_CURSO cuya fecha de salida es la fecha pedida (7.1, 7.6), ordenadas por "
                    + "apartamento, 10 por página. Sin fecha se usa hoy en la zona horaria del alojamiento")
    @ApiResponse(responseCode = "200", description = "Página de salidas (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa o fecha mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    public ResponseEntity<PaginaResponse<ReservaResumenResponse>> salidas(
            @RequestParam(required = false) LocalDate fecha,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        Pagina<Reserva> reservas = listarLlegadasYSalidasUseCase.salidas(fecha, pagina);
        return ResponseEntity.ok(PaginaMapper.aResponse(reservas, mapper::aResumen));
    }

    /** CU-18. GET /api/reservas/{codigo} */
    @GetMapping("/{codigo}")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR') "
            + "or (hasAuthority('HUESPED') and @propietarioReserva.esTitular(#codigo, authentication))")
    @Operation(tags = TAG_RESERVAS, summary ="Consultar una reserva")
    @ApiResponse(responseCode = "200", description = "Detalle de la reserva")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite o no es el titular de la reserva")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "El código no tiene el formato RES-YYYY-NNNNN")
    public ResponseEntity<ReservaDetalleResponse> consultar(@PathVariable String codigo) {
        Reserva reserva = consultarReservaUseCase.ejecutar(new CodigoReserva(codigo));
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** CU-11. PUT /api/reservas/{codigo} */
    @PutMapping("/{codigo}")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RESERVAS, summary ="Modificar una reserva",
            description = "Cambia fechas, ocupantes, apartamento o mascotas de una reserva no iniciada; revalida con "
                    + "las tarifas vigentes y asienta la diferencia en el folio (RN-14) y, si ahora trae mascotas, el "
                    + "cargo de aseo (L-16). Un campo en null se conserva")
    @ApiResponse(responseCode = "200", description = "Reserva modificada")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "Una regla del negocio lo impide (RN-01 a RN-08, RN-14, L-16)")
    public ResponseEntity<ModificacionReservaResponse> modificar(@PathVariable String codigo,
                                                                 @Valid @RequestBody ModificarReservaRequest request) {
        CodigoReserva codigoReserva = new CodigoReserva(codigo);
        Dinero diferencia = modificarReservaUseCase.ejecutar(codigoReserva,
                mapper.aIdentificacion(request.identificacionApartamento()),
                request.fechaEntrada(), request.fechaSalida(),
                request.ocupantes() == null ? null : mapper.aOcupantes(request.ocupantes()),
                request.cantidadMascotas(), request.pesoMascotaMasPesadaKg());
        Reserva reserva = consultarReservaUseCase.ejecutar(codigoReserva);
        return ResponseEntity.ok(new ModificacionReservaResponse(mapper.aDetalle(reserva), mapper.aPesos(diferencia)));
    }

    /** CU-10. PUT /api/reservas/{codigo}/confirmar */
    @PutMapping("/{codigo}/confirmar")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RESERVAS, summary ="Confirmar una reserva",
            description = "PENDIENTE → CONFIRMADA. Exige hora estimada de llegada (RN-09), plazo vigente (RN-21) "
                    + "y anticipo del 50 % cubierto en el folio (L-11). El cuerpo es opcional")
    @ApiResponse(responseCode = "200", description = "Reserva confirmada")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "Transición inválida, falta la hora, venció el plazo o falta anticipo")
    public ResponseEntity<ReservaDetalleResponse> confirmar(@PathVariable String codigo,
                                                            @Valid @RequestBody(required = false) ConfirmarReservaRequest request) {
        Reserva reserva = confirmarReservaUseCase.ejecutar(new CodigoReserva(codigo),
                request == null ? null : request.horaEstimadaLlegada());
        return ResponseEntity.ok(mapper.aDetalle(reserva));
    }

    /** CU-09. PUT /api/reservas/{codigo}/cancelar */
    @PutMapping("/{codigo}/cancelar")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR') "
            + "or (hasAuthority('HUESPED') and @propietarioReserva.esTitular(#codigo, authentication))")
    @Operation(tags = TAG_RESERVAS, summary ="Cancelar una reserva",
            description = "Calcula la retención con la política congelada (RN-13), la asienta en el folio y libera "
                    + "las noches de inmediato (RN-12)")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada, con la retención aplicada")
    @ApiResponse(responseCode = "400", description = "Falta el motivo")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite o no es el titular de la reserva")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "La reserva ya inició o está en un estado terminal (RN-08)")
    public ResponseEntity<RetencionResponse> cancelar(@PathVariable String codigo,
                                                      @Valid @RequestBody CancelarReservaRequest request) {
        Dinero retencion = cancelarReservaUseCase.ejecutar(new CodigoReserva(codigo), request.motivo());
        return ResponseEntity.ok(new RetencionResponse(codigo, EstadoReserva.CANCELADA, mapper.aPesos(retencion)));
    }

    /** CU-12. PUT /api/reservas/{codigo}/no-show */
    @PutMapping("/{codigo}/no-show")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RESERVAS, summary ="Declarar no-show",
            description = "Desde las 22:00 del día de entrada (L-15) marca NO_SHOW, retiene el 50 % del total "
                    + "(RN-13) y libera las noches (RN-12)")
    @ApiResponse(responseCode = "200", description = "No-show declarado, con la retención aplicada")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "Aún no es la hora límite o la reserva no está CONFIRMADA")
    public ResponseEntity<RetencionResponse> declararNoShow(@PathVariable String codigo) {
        Dinero retencion = declararNoShowUseCase.ejecutar(new CodigoReserva(codigo));
        return ResponseEntity.ok(new RetencionResponse(codigo, EstadoReserva.NO_SHOW, mapper.aPesos(retencion)));
    }

    /** CU-04. PUT /api/reservas/{codigo}/registrar-llegada: cambia también el estado del apartamento. */
    @PutMapping("/{codigo}/registrar-llegada")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RECEPCION, summary ="Registrar la llegada",
            description = "La reserva pasa a EN_CURSO y el apartamento a OCUPADO. Exige reserva CONFIRMADA con la "
                    + "fecha de entrada alcanzada (RN-10) y apartamento PREPARADO (RN-11)")
    @ApiResponse(responseCode = "200", description = "Llegada registrada")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "La reserva no está CONFIRMADA, no es su fecha o el apartamento no está PREPARADO")
    public ResponseEntity<ReservaDetalleResponse> registrarLlegada(@PathVariable String codigo) {
        CodigoReserva codigoReserva = new CodigoReserva(codigo);
        registrarLlegadaUseCase.ejecutar(codigoReserva);
        return ResponseEntity.ok(mapper.aDetalle(consultarReservaUseCase.ejecutar(codigoReserva)));
    }

    /** CU-05. PUT /api/reservas/{codigo}/registrar-salida: cambia también el estado del apartamento. */
    @PutMapping("/{codigo}/registrar-salida")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RECEPCION, summary ="Registrar la salida",
            description = "La reserva pasa a FINALIZADA (RN-12) y el apartamento a PENDIENTE_PREPARACION. El folio "
                    + "debe quedar cerrado: saldo en cero o autorización del administrador (RN-17)")
    @ApiResponse(responseCode = "200", description = "Salida registrada")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "No existe una reserva con ese código")
    @ApiResponse(responseCode = "409", description = "La reserva no está EN_CURSO o el folio tiene saldo sin autorizar")
    public ResponseEntity<ReservaDetalleResponse> registrarSalida(@PathVariable String codigo) {
        CodigoReserva codigoReserva = new CodigoReserva(codigo);
        registrarSalidaUseCase.ejecutar(codigoReserva);
        return ResponseEntity.ok(mapper.aDetalle(consultarReservaUseCase.ejecutar(codigoReserva)));
    }

    /** CU-27. GET /api/reservas/calendario?apartamento=&desde=&hasta=: combina reservas y bloqueos. */
    @GetMapping("/calendario")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(tags = TAG_RECEPCION, summary ="Calendario de ocupación de un apartamento",
            description = "Estado de cada noche entre desde (incluida) y hasta (excluida): LIBRE, RESERVADA (con el "
                    + "código y el estado de la reserva activa) o BLOQUEADA (con el motivo del bloqueo vigente). "
                    + "Máximo 62 noches por consulta (decisión del equipo)")
    @ApiResponse(responseCode = "200", description = "Calendario noche por noche")
    @ApiResponse(responseCode = "400", description = "Falta un parámetro o una fecha está mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "No existe el apartamento")
    @ApiResponse(responseCode = "409", description = "desde no es anterior a hasta o el rango supera 62 noches (7.1)")
    public ResponseEntity<CalendarioOcupacionResponse> calendario(@RequestParam String apartamento,
                                                                  @RequestParam LocalDate desde,
                                                                  @RequestParam LocalDate hasta) {
        IdentificacionApartamento identificacion = new IdentificacionApartamento(apartamento);
        List<NocheCalendario> noches = consultarCalendarioUseCase.ejecutar(identificacion, desde, hasta);
        return ResponseEntity.ok(ocupacionMapper.aCalendario(identificacion, desde, hasta, noches));
    }
}
