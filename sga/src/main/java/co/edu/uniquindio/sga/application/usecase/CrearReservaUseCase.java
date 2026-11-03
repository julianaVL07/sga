package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.service.CreacionReservaService;
import co.edu.uniquindio.sga.domain.service.PoliticaCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Mascotas;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.Titular;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * CU-03. POST /api/reservas
 * Crea la reserva en PENDIENTE, abre su folio con el depósito (RP-02) y, si la llegada es nocturna, el recargo
 * (RP-03), y congela cotización y política (RN-22). Incluye CU-01 y CU-02 (disponibilidad y cotización).
 * Actores: huésped (canal PORTAL) y recepcionista (canal DIRECTO). El canal EXTERNO entra por CU-08; esa regla
 * (RN-19) y el canal PORTAL por defecto los decide CreacionReservaService. También valida las mascotas (L-16).
 */

@Service
public class CrearReservaUseCase {

    /**
     * Datos de la solicitud. La cotización es la que vio el titular; puede venir null.
     * Las mascotas (L-16) pueden venir null: equivale a sin mascotas.
     */
    public record SolicitudReserva(IdentificacionApartamento apartamento, LocalDate fechaEntrada, LocalDate fechaSalida,
                                   Titular titular, List<Ocupante> ocupantes, LocalTime horaEstimadaLlegada,
                                   Cotizacion cotizacion, CanalOrigen canal, Mascotas mascotas) {
    }

    private final CreacionReservaService creacionReservaService;
    private final PoliticaCancelacionService politicaCancelacionService;
    private final Clock reloj;

    /**
     * @param politicaCancelacionService entrega la política de cancelación vigente en el repositorio, que se congela
     *                                   en cada reserva nueva (RN-22). Cambia cuando se publica una versión (CU-30).
     * @param reloj           reloj con zona horaria de Colombia (3.1).
     */
    public CrearReservaUseCase(CreacionReservaService creacionReservaService,
                               PoliticaCancelacionService politicaCancelacionService, Clock reloj) {
        this.creacionReservaService = Objects.requireNonNull(creacionReservaService);
        this.politicaCancelacionService = Objects.requireNonNull(politicaCancelacionService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Transactional
    public Reserva ejecutar(SolicitudReserva solicitud) {
        if (solicitud == null) throw new ReglaDominioException("La solicitud de reserva es obligatoria");
        return creacionReservaService.registrarReservaPropia(solicitud.apartamento(),
                new Estancia(solicitud.fechaEntrada(), solicitud.fechaSalida()), solicitud.titular(),
                solicitud.ocupantes(), solicitud.horaEstimadaLlegada(), solicitud.cotizacion(),
                politicaCancelacionService.vigente(), solicitud.canal(), solicitud.mascotas(),
                LocalDateTime.now(reloj));
    }
}
