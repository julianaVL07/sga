package co.edu.uniquindio.sga.infrastructure.rest;

import co.edu.uniquindio.sga.application.dto.request.CotizarEstanciaRequest;
import co.edu.uniquindio.sga.application.dto.response.CotizacionResponse;
import co.edu.uniquindio.sga.application.usecase.CotizarEstanciaUseCase;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapper;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador REST de CU-02 (Guía 08). Es POST aunque no guarda nada porque los ocupantes, con su fecha de
 * nacimiento, viajan en el cuerpo. Los ocupantes se convierten con el mismo ReservaMapper de CU-03.
 */
@RestController
@RequestMapping("/api/cotizaciones")
@RequiredArgsConstructor
@Tag(name = "Cotizaciones", description = "Valor de una estancia en el Apartahotel Coral antes de reservar")
public class CotizacionController {

    private final CotizarEstanciaUseCase cotizarEstanciaUseCase;
    private final ReservaMapper reservaMapper;
    private final ApartamentoMapper apartamentoMapper;

    /** CU-02. POST /api/cotizaciones */
    @PostMapping
    @Operation(summary = "Cotizar una estancia",
            description = "Calcula noche a noche con la tarifa de cada temporada (RN-06, RP-01) y solo cobra a los "
                    + "ocupantes desde 6 años (RN-05). No crea la reserva")
    @ApiResponse(responseCode = "200", description = "Cotización con el detalle por noche")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @ApiResponse(responseCode = "404", description = "No existe un apartamento con esa identificación")
    @ApiResponse(responseCode = "409", description = "Fechas inválidas, estancia mínima o apartamento sin tarifa")
    public ResponseEntity<CotizacionResponse> cotizar(@Valid @RequestBody CotizarEstanciaRequest request) {
        Cotizacion cotizacion = cotizarEstanciaUseCase.ejecutar(
                new IdentificacionApartamento(request.identificacionApartamento()),
                request.fechaEntrada(), request.fechaSalida(),
                reservaMapper.aOcupantes(request.ocupantes()));
        return ResponseEntity.ok(apartamentoMapper.aCotizacionResponse(request.identificacionApartamento(),
                request.fechaEntrada(), request.fechaSalida(), cotizacion));
    }
}