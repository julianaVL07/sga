package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * CU-01. GET /api/apartamentos/disponibles?entrada=&salida=&ocupantes=
 * Solo consulta: no modifica datos. Reglas RN-01, RN-02, RN-07, RN-20 y RP-01 (en DisponibilidadApartamentoService).
 * Actores: huésped y recepcionista.
 */
public class BuscarApartamentoDisponibleUseCase {

    private final DisponibilidadApartamentoService disponibilidadService;

    public BuscarApartamentoDisponibleUseCase(DisponibilidadApartamentoService disponibilidadService) {
        this.disponibilidadService = Objects.requireNonNull(disponibilidadService);
    }

    public List<Apartamento> ejecutar(LocalDate fechaEntrada, LocalDate fechaSalida, int totalOcupantes) {
        return disponibilidadService.buscarDisponibles(new Estancia(fechaEntrada, fechaSalida), totalOcupantes);
    }
}
