package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.CrearCanalVentaRequest;
import co.edu.uniquindio.sga.application.dto.request.ResolverConflictoCanalRequest;
import co.edu.uniquindio.sga.application.dto.response.CanalVentaResponse;
import co.edu.uniquindio.sga.application.dto.response.ConflictoCanalResponse;
import co.edu.uniquindio.sga.application.dto.response.CredencialCanalResponse;
import co.edu.uniquindio.sga.application.dto.response.EventoBitacoraResponse;
import co.edu.uniquindio.sga.application.dto.response.PaginaResponse;
import co.edu.uniquindio.sga.application.usecase.BitacoraCanalUseCase;
import co.edu.uniquindio.sga.application.usecase.CanalesVentaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConflictosCanalUseCase;
import co.edu.uniquindio.sga.domain.valueobject.EstadoConflictoCanal;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.CanalVentaMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.PaginaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.time.LocalDate;

/**
 * Administración de los canales de venta externos (10.3.1, 7.8): los canales y su credencial, los conflictos que
 * dejan sus reservas (RN-18) y la bitácora de mensajes. Solo ADMINISTRADOR, con su JWT.
 * Las tres operaciones del canal (/api/canales/{canal}/...) están en CanalExternoController y usan la credencial
 * del canal, no el JWT.
 */
@RestController
@RequestMapping("/api/canales")
@RequiredArgsConstructor
@Tag(name = "Canales de venta", description = "Canales externos, su credencial, conflictos y bitácora (10.3.1, 7.8)")
public class CanalVentaController {

    private final CanalesVentaUseCase canalesVentaUseCase;
    private final ConflictosCanalUseCase conflictosCanalUseCase;
    private final BitacoraCanalUseCase bitacoraCanalUseCase;
    private final CanalVentaMapper mapper;

    /** POST /api/canales */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Crear un canal de venta",
            description = "Registra la plataforma externa y genera su credencial al azar. La credencial solo se "
                    + "muestra en esta respuesta: el SGA guarda su hash (BCrypt)")
    @ApiResponse(responseCode = "201", description = "Canal creado; trae la credencial en claro una sola vez")
    @ApiResponse(responseCode = "400", description = "Nombre vacío o con un formato inválido")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "Ya existe un canal con ese nombre o el nombre está reservado")
    public ResponseEntity<CredencialCanalResponse> crear(@Valid @RequestBody CrearCanalVentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.aResponse(canalesVentaUseCase.crear(request.nombre())));
    }

    /** GET /api/canales?pagina=0 */
    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Listar canales de venta",
            description = "Activos e inactivos, por nombre, 10 por página (12.4). Nunca trae la credencial")
    @ApiResponse(responseCode = "200", description = "Página de canales (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    public ResponseEntity<PaginaResponse<CanalVentaResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(PaginaMapper.aResponse(canalesVentaUseCase.listar(pagina), mapper::aResponse));
    }

    /** PUT /api/canales/{nombre}/regenerar-credencial */
    @PutMapping("/{nombre}/regenerar-credencial")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Regenerar la credencial de un canal",
            description = "Emite una credencial nueva y la anterior deja de servir. La nueva solo se muestra en esta "
                    + "respuesta")
    @ApiResponse(responseCode = "200", description = "Credencial nueva en claro, una sola vez")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "El canal no existe")
    @ApiResponse(responseCode = "409", description = "El canal está inactivo")
    public ResponseEntity<CredencialCanalResponse> regenerarCredencial(@PathVariable String nombre) {
        return ResponseEntity.ok(mapper.aResponse(canalesVentaUseCase.regenerarCredencial(nombre)));
    }

    /** PUT /api/canales/{nombre}/desactivar */
    @PutMapping("/{nombre}/desactivar")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Desactivar un canal",
            description = "Eliminación lógica: el canal y su historia se conservan, pero sus peticiones responden "
                    + "403 desde ese momento")
    @ApiResponse(responseCode = "200", description = "Canal desactivado")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "El canal no existe")
    @ApiResponse(responseCode = "409", description = "El canal ya estaba inactivo")
    public ResponseEntity<CanalVentaResponse> desactivar(@PathVariable String nombre) {
        return ResponseEntity.ok(mapper.aResponse(canalesVentaUseCase.desactivar(nombre)));
    }

    /** GET /api/canales/conflictos?estado=&pagina=0 */
    @GetMapping("/conflictos")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Listar conflictos de canal",
            description = "Reservas externas rechazadas por chocar con una vigente (RN-18), de la más reciente a la "
                    + "más antigua, 10 por página (12.4). Filtro opcional por estado")
    @ApiResponse(responseCode = "200", description = "Página de conflictos (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa o estado mal escrito")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    public ResponseEntity<PaginaResponse<ConflictoCanalResponse>> listarConflictos(
            @RequestParam(required = false) EstadoConflictoCanal estado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(PaginaMapper.aResponse(conflictosCanalUseCase.listar(estado, pagina),
                mapper::aResponse));
    }

    /** PUT /api/canales/conflictos/{id}/resolver */
    @PutMapping("/conflictos/{id}/resolver")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Resolver un conflicto de canal",
            description = "Marca el conflicto como RESUELTO con el motivo y el administrador del token. Nunca toca "
                    + "la reserva vigente (RN-18)")
    @ApiResponse(responseCode = "200", description = "Conflicto resuelto")
    @ApiResponse(responseCode = "400", description = "Falta el motivo")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "404", description = "El conflicto no existe")
    @ApiResponse(responseCode = "409", description = "El conflicto ya estaba resuelto")
    public ResponseEntity<ConflictoCanalResponse> resolverConflicto(
            @PathVariable String id,
            @Valid @RequestBody ResolverConflictoCanalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(mapper.aResponse(
                conflictosCanalUseCase.resolver(id, request.motivo(), jwt.getSubject())));
    }

    /** GET /api/canales/eventos?canal=&desde=&hasta=&pagina=0 */
    @GetMapping("/eventos")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Consultar la bitácora de los canales",
            description = "Todos los mensajes de los canales (7.8): aceptados, rechazados e intentos sin credencial "
                    + "válida, del más reciente al más antiguo, 10 por página (12.4). Filtros opcionales por canal y "
                    + "por fechas (incluidas)")
    @ApiResponse(responseCode = "200", description = "Página de eventos (puede venir vacía)")
    @ApiResponse(responseCode = "400", description = "Página negativa o fecha mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "La fecha desde es posterior a la fecha hasta")
    public ResponseEntity<PaginaResponse<EventoBitacoraResponse>> listarEventos(
            @RequestParam(required = false) String canal,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina) {
        return ResponseEntity.ok(PaginaMapper.aResponse(bitacoraCanalUseCase.listar(canal, desde, hasta, pagina),
                mapper::aResponse));
    }
}
