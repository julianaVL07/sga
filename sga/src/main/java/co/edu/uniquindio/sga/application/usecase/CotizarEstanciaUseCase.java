package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.service.CotizadorEstanciaService;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * CU-02. POST /api/cotizaciones
 * Calcula el valor noche por noche con su desglose, sin guardar nada (RN-05, RN-06).
 * Usa el umbral de edad configurado en el alojamiento (L-09). Actores: huésped y recepcionista.
 */
public class CotizarEstanciaUseCase {

    private final CotizadorEstanciaService cotizadorService;

    public CotizarEstanciaUseCase(CotizadorEstanciaService cotizadorService) {
        this.cotizadorService = Objects.requireNonNull(cotizadorService);
    }

    public Cotizacion ejecutar(IdentificacionApartamento apartamento, LocalDate fechaEntrada, LocalDate fechaSalida,
                               List<Ocupante> ocupantes) {
        return cotizadorService.cotizar(apartamento, new Estancia(fechaEntrada, fechaSalida), ocupantes, null);
    }
}
