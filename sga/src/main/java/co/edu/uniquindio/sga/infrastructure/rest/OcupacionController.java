package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.response.CalendarioOcupacionResponse;
import co.edu.uniquindio.sga.application.usecase.ConsultarCalendarioOcupacionUseCase;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.OcupacionMapper;
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
import java.util.List;

/**
 * Adaptador REST del calendario de ocupación (Guía 08): CU-27. Comparte la ruta /api/reservas con
 * ReservaController, pero va en su propia clase porque combina reservas y bloqueos del apartamento.
 * No tiene reglas ni try/catch: los errores los traduce GlobalExceptionHandler (400, 404, 409).
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Operación en recepción", description = "Registro de llegada y salida del grupo")
public class OcupacionController {

    private final ConsultarCalendarioOcupacionUseCase consultarCalendarioUseCase;
    private final OcupacionMapper mapper;

    /** CU-27. GET /api/reservas/calendario?apartamento=&desde=&hasta= */
    @GetMapping("/calendario")
    @PreAuthorize("hasAnyAuthority('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Calendario de ocupación de un apartamento",
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
        return ResponseEntity.ok(mapper.aCalendario(identificacion, desde, hasta, noches));
    }
}
