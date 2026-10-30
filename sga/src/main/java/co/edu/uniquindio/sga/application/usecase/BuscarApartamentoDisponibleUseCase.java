package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.ApartamentoDisponible;
import co.edu.uniquindio.sga.domain.valueobject.CriterioBusqueda;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * CU-01. GET /api/apartamentos/disponibles?entrada=&salida=&ocupantes=
 * Solo consulta: no modifica datos. Reglas RN-01, RN-02, RN-07, RN-20 y RP-01 (en DisponibilidadApartamentoService).
 * Actores: huésped y recepcionista.
 */

@Service
public class BuscarApartamentoDisponibleUseCase {

    private final DisponibilidadApartamentoService disponibilidadService;
    private final int tamanoPagina;

    public BuscarApartamentoDisponibleUseCase(DisponibilidadApartamentoService disponibilidadService,
                                              @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.disponibilidadService = Objects.requireNonNull(disponibilidadService);
        this.tamanoPagina = tamanoPagina;
    }

    @Transactional (readOnly = true)
    public List<Apartamento> ejecutar(LocalDate fechaEntrada, LocalDate fechaSalida, int totalOcupantes) {
        return disponibilidadService.buscarDisponibles(new Estancia(fechaEntrada, fechaSalida), totalOcupantes);
    }

    /**
     * CU-01 con filtros y paginación (7.5). La disponibilidad la decide el dominio y la página se corta
     * sobre ese resultado (12.4: 10 por página).
     *
     * @param fechasNacimiento opcional; sin fechas todos los ocupantes cuentan como facturables en el estimado.
     */
    @Transactional (readOnly = true)
    public Pagina<ApartamentoDisponible> buscar(LocalDate fechaEntrada, LocalDate fechaSalida, int totalOcupantes,
                                                List<LocalDate> fechasNacimiento, CriterioBusqueda criterio,
                                                int pagina) {
        List<ApartamentoDisponible> disponibles = disponibilidadService.buscarDisponibles(
                new Estancia(fechaEntrada, fechaSalida), totalOcupantes, fechasNacimiento, criterio);
        return Pagina.de(disponibles, pagina, tamanoPagina);
    }
}
