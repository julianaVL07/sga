package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.AutorizarCierreFolioRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarCargoServicioRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarPagoRequest;
import co.edu.uniquindio.sga.application.dto.response.FolioResponse;
import co.edu.uniquindio.sga.application.usecase.AutorizarCierreFolioUseCase;
import co.edu.uniquindio.sga.application.usecase.CerrarFolioUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarFolioUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarCargoServicioUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarPagoUseCase;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.FolioMapper;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Adaptador REST del folio de una reserva (Guía 08): consulta del folio, CU-06, CU-07, CU-15 y cargos de servicios
 * adicionales (7.1).
 * El folio cuelga de la reserva: /api/reservas/{codigo}/folio. Todas las operaciones responden con el folio
 * actualizado para mostrar el saldo. La fecha la pone el reloj del SGA, nunca el cliente.
 */
@RestController
@RequestMapping("/api/reservas/{codigo}/folio")
@RequiredArgsConstructor
@Tag(name = "Folio", description = "Cuenta de la reserva: cargos, pagos, saldo y cierre")
public class FolioController {

    private final CerrarFolioUseCase cerrarFolioUseCase;
    private final AutorizarCierreFolioUseCase autorizarCierreFolioUseCase;
    private final RegistrarPagoUseCase registrarPagoUseCase;
    private final ConsultarFolioUseCase consultarFolioUseCase;
    private final RegistrarCargoServicioUseCase registrarCargoServicioUseCase;
    private final FolioMapper mapper;

    /** GET /api/reservas/{codigo}/folio: el recurso al que apunta la Location de un pago registrado. */
    @GetMapping
    @Operation(summary = "Consultar el folio",
            description = "Cargos, pagos, saldo y estado de cierre del folio de la reserva (7.7)")
    @ApiResponse(responseCode = "200", description = "El folio de la reserva")
    @ApiResponse(responseCode = "404", description = "No existe la reserva o su folio")
    @ApiResponse(responseCode = "409", description = "Código de reserva mal formado")
    public ResponseEntity<FolioResponse> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(mapper.aResponse(consultarFolioUseCase.ejecutar(new CodigoReserva(codigo))));
    }

    /** CU-15. POST /api/reservas/{codigo}/folio/pagos */
    @PostMapping("/pagos")
    @Operation(summary = "Registrar un pago",
            description = "Abono contra el folio con su medio y su fecha (RN-15). Admite pagos parciales; un pago "
                    + "no se edita ni se borra (RN-16)")
    @ApiResponse(responseCode = "201", description = "Pago registrado; devuelve el folio con el nuevo saldo y "
            + "Location apunta al folio")
    @ApiResponse(responseCode = "400", description = "Falta el valor o el medio, o el valor no es positivo")
    @ApiResponse(responseCode = "404", description = "No existe la reserva o su folio")
    @ApiResponse(responseCode = "409", description = "El folio ya está cerrado")
    public ResponseEntity<FolioResponse> registrarPago(@PathVariable String codigo,
                                                       @Valid @RequestBody RegistrarPagoRequest request) {
        Folio folio = registrarPagoUseCase.ejecutar(new CodigoReserva(codigo), Dinero.de(request.valor()),
                request.medio());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reservas/{codigo}/folio")
                .buildAndExpand(folio.getReserva().valor())
                .toUri();
        return ResponseEntity.created(location).body(mapper.aResponse(folio));
    }

    /** 7.1. POST /api/reservas/{codigo}/folio/cargos */
    @PostMapping("/cargos")
    @Operation(summary = "Registrar el cargo de un servicio adicional",
            description = "Asienta en el folio el cargo de un servicio adicional del alojamiento (L-17), con su valor y "
                    + "la fecha de hoy. El cargo no se edita ni se borra; se corrige con un movimiento inverso (RN-16)")
    @ApiResponse(responseCode = "201", description = "Cargo registrado; devuelve el folio con el nuevo saldo y "
            + "Location apunta al folio")
    @ApiResponse(responseCode = "400", description = "Falta el id del servicio")
    @ApiResponse(responseCode = "404", description = "No existe la reserva o su folio")
    @ApiResponse(responseCode = "409", description = "El servicio no existe, no está activo o no genera cargo (L-17), "
            + "o el folio está cerrado (RN-16)")
    public ResponseEntity<FolioResponse> registrarCargo(@PathVariable String codigo,
                                                        @Valid @RequestBody RegistrarCargoServicioRequest request) {
        Folio folio = registrarCargoServicioUseCase.ejecutar(new CodigoReserva(codigo), request.idServicio());
        return ResponseEntity.created(ubicacionDelFolio(folio)).body(mapper.aResponse(folio));
    }

    /** CU-06. PUT /api/reservas/{codigo}/folio/cerrar */
    @PutMapping("/cerrar")
    @Operation(summary = "Cerrar el folio",
            description = "Solo si el saldo es exactamente cero (RN-17). Con saldo se usa la autorización del "
                    + "administrador (CU-07)")
    @ApiResponse(responseCode = "200", description = "Folio cerrado")
    @ApiResponse(responseCode = "404", description = "No existe la reserva o su folio")
    @ApiResponse(responseCode = "409", description = "El saldo no es cero o el folio ya estaba cerrado")
    public ResponseEntity<FolioResponse> cerrar(@PathVariable String codigo) {
        Folio folio = cerrarFolioUseCase.ejecutar(new CodigoReserva(codigo));
        return ResponseEntity.ok(mapper.aResponse(folio));
    }

    /** CU-07. POST /api/reservas/{codigo}/folio/autorizacion-cierre */
    @PostMapping("/autorizacion-cierre")
    @Operation(summary = "Autorizar el cierre con saldo",
            description = "El administrador cierra el folio aunque tenga saldo; queda registrado quién autoriza y "
                    + "por qué (RN-17)")
    @ApiResponse(responseCode = "200", description = "Folio cerrado con autorización")
    @ApiResponse(responseCode = "400", description = "Falta quién autoriza o el motivo")
    @ApiResponse(responseCode = "404", description = "No existe la reserva o su folio")
    @ApiResponse(responseCode = "409", description = "El folio ya estaba cerrado")
    public ResponseEntity<FolioResponse> autorizarCierre(@PathVariable String codigo,
                                                         @Valid @RequestBody AutorizarCierreFolioRequest request) {
        Folio folio = autorizarCierreFolioUseCase.ejecutar(new CodigoReserva(codigo), request.autorizadoPor(),
                request.motivo());
        return ResponseEntity.ok(mapper.aResponse(folio));
    }

    /** Location de un movimiento nuevo: el folio de la reserva, que se consulta con GET. */
    private static URI ubicacionDelFolio(Folio folio) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reservas/{codigo}/folio")
                .buildAndExpand(folio.getReserva().valor())
                .toUri();
    }
}
