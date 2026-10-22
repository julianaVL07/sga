package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.AutorizarCierreFolioRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarPagoRequest;
import co.edu.uniquindio.sga.application.dto.response.FolioResponse;
import co.edu.uniquindio.sga.application.usecase.AutorizarCierreFolioUseCase;
import co.edu.uniquindio.sga.application.usecase.CerrarFolioUseCase;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador REST del folio de una reserva (Guía 08): CU-06, CU-07 y CU-15.
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
    private final FolioMapper mapper;

    /** CU-15. POST /api/reservas/{codigo}/folio/pagos */
    @PostMapping("/pagos")
    @Operation(summary = "Registrar un pago",
            description = "Abono contra el folio con su medio y su fecha (RN-15). Admite pagos parciales; un pago "
                    + "no se edita ni se borra (RN-16)")
    @ApiResponse(responseCode = "201", description = "Pago registrado; devuelve el folio con el nuevo saldo")
    @ApiResponse(responseCode = "400", description = "Falta el valor o el medio, o el valor no es positivo")
    @ApiResponse(responseCode = "404", description = "No existe la reserva o su folio")
    @ApiResponse(responseCode = "409", description = "El folio ya está cerrado")
    public ResponseEntity<FolioResponse> registrarPago(@PathVariable String codigo,
                                                       @Valid @RequestBody RegistrarPagoRequest request) {
        Folio folio = registrarPagoUseCase.ejecutar(new CodigoReserva(codigo), Dinero.de(request.valor()),
                request.medio());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.aResponse(folio));
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
}