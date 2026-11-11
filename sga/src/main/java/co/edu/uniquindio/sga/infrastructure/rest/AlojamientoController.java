package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.ActualizarAlojamientoRequest;
import co.edu.uniquindio.sga.application.dto.response.AlojamientoResponse;
import co.edu.uniquindio.sga.application.usecase.ConfiguracionAlojamientoUseCase;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ConfiguracionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador REST del alojamiento (Guía 08): CU-28. El sistema administra un único alojamiento (F-13), por eso la
 * ruta no lleva id. No tiene reglas ni try/catch: los errores los traduce GlobalExceptionHandler (400, 409).
 */
@RestController
@RequestMapping("/api/alojamiento")
@RequiredArgsConstructor
@Tag(name = "Configuración del alojamiento", description = "Alojamiento, temporadas y política de cancelación")
public class AlojamientoController {

    private final ConfiguracionAlojamientoUseCase configuracionAlojamientoUseCase;
    private final ConfiguracionMapper mapper;
    /** RP-02: el mismo depósito que se asienta en el folio (bean de DomainServiceConfig) */
    private final DepositoGarantia depositoGarantia;

    /** CU-28. GET /api/alojamiento */
    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Consultar el alojamiento",
            description = "Datos, ubicación, horario, normas de convivencia, servicios adicionales y parámetros "
                    + "vigentes de la sección 6 (solo lectura: se configuran en application.properties)")
    @ApiResponse(responseCode = "200", description = "El alojamiento")
    public ResponseEntity<AlojamientoResponse> consultar() {
        return ResponseEntity.ok(mapper.aAlojamientoResponse(configuracionAlojamientoUseCase.consultar(),
                depositoGarantia));
    }

    /** CU-28. PUT /api/alojamiento */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR')")
    @Operation(summary = "Actualizar los datos del alojamiento",
            description = "Reemplaza nombre, descripción, ciudad, dirección, ubicación y normas de convivencia. El "
                    + "horario (L-12) es opcional: si no llega ninguna hora se conserva el vigente. Los parámetros "
                    + "de la sección 6 no se cambian por API")
    @ApiResponse(responseCode = "200", description = "Alojamiento actualizado")
    @ApiResponse(responseCode = "400", description = "Falta un dato obligatorio o una hora está mal escrita")
    @ApiResponse(responseCode = "401", description = "Sin token o token inválido")
    @ApiResponse(responseCode = "403", description = "El rol no lo permite")
    @ApiResponse(responseCode = "409", description = "Coordenada fuera de rango (7.3) o falta una de las dos horas "
            + "del horario (L-12)")
    public ResponseEntity<AlojamientoResponse> actualizar(@Valid @RequestBody ActualizarAlojamientoRequest request) {
        Alojamiento alojamiento = configuracionAlojamientoUseCase.actualizar(mapper.aDatos(request));
        return ResponseEntity.ok(mapper.aAlojamientoResponse(alojamiento, depositoGarantia));
    }
}
