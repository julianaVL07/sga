package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.service.CreacionReservaService;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.Titular;

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
 * Actores: huésped (canal PORTAL) y recepcionista (canal DIRECTO). El canal EXTERNO entra por CU-08.
 */
public class CrearReservaUseCase {

    /** Datos de la solicitud. La cotización es la que vio el titular; puede venir null. */
    public record SolicitudReserva(IdentificacionApartamento apartamento, LocalDate fechaEntrada, LocalDate fechaSalida,
                                   Titular titular, List<Ocupante> ocupantes, LocalTime horaEstimadaLlegada,
                                   Cotizacion cotizacion, CanalOrigen canal) {
    }

    private final CreacionReservaService creacionReservaService;
    private final PoliticaCancelacion politicaVigente;
    private final Clock reloj;

    /**
     * @param politicaVigente política de cancelación vigente que se congela en cada reserva nueva (RN-22).
     * @param reloj           reloj con zona horaria de Colombia (3.1).
     */
    public CrearReservaUseCase(CreacionReservaService creacionReservaService, PoliticaCancelacion politicaVigente,
                               Clock reloj) {
        this.creacionReservaService = Objects.requireNonNull(creacionReservaService);
        this.politicaVigente = Objects.requireNonNull(politicaVigente);
        this.reloj = Objects.requireNonNull(reloj);
    }

    public Reserva ejecutar(SolicitudReserva solicitud) {
        if (solicitud == null) throw new ReglaDominioException("La solicitud de reserva es obligatoria");
        if (solicitud.canal() == CanalOrigen.EXTERNO) {
            throw new ReglaDominioException("RN-19: Las reservas del canal externo se reciben por RecibirReservaExternaUseCase");
        }
        CanalOrigen canal = solicitud.canal() == null ? CanalOrigen.PORTAL : solicitud.canal();
        return creacionReservaService.registrarNuevaReserva(solicitud.apartamento(),
                new Estancia(solicitud.fechaEntrada(), solicitud.fechaSalida()), solicitud.titular(),
                solicitud.ocupantes(), solicitud.horaEstimadaLlegada(), solicitud.cotizacion(), politicaVigente,
                canal, null, LocalDateTime.now(reloj));
    }
}
