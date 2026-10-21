package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService.DatosReservaExterna;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * CU-08. GET /api/canales/{canal}/disponibilidad, POST /api/canales/{canal}/reservas y
 * PUT /api/canales/{canal}/reservas/{identificadorExterno}/cancelar (las tres operaciones de 10.3.1).
 * Recibe los mensajes del canal externo (plataformas de terceros, 10.3.1). Un mensaje repetido no crea otra reserva (RN-19);
 * si las noches ya estaban vendidas o bloqueadas se rechaza y se registra un conflicto (RN-18). Si procede,
 * incluye CU-03 (la reserva nace PENDIENTE con su folio). Todo mensaje queda en la bitácora (7.8).
 * Actor: canal externo.
 */
public class RecibirReservaExternaUseCase {

    private final ConciliacionCanalExternoService conciliacionService;
    private final Clock reloj;

    public RecibirReservaExternaUseCase(ConciliacionCanalExternoService conciliacionService, Clock reloj) {
        this.conciliacionService = Objects.requireNonNull(conciliacionService);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** Consulta de disponibilidad del canal por rango de fechas y composición del grupo (10.3.1, operación 1). */
    public List<Apartamento> consultarDisponibilidad(LocalDate fechaEntrada, LocalDate fechaSalida,
                                                     int totalOcupantes) {
        return conciliacionService.consultarDisponibilidad(CanalOrigen.EXTERNO,
                new Estancia(fechaEntrada, fechaSalida), totalOcupantes, LocalDateTime.now(reloj));
    }

    /** Creación de una reserva enviada por el canal. */
    public ResultadoEventoCanal ejecutar(String identificadorExterno, DatosReservaExterna datos) {
        return conciliacionService.procesarMensajeExterno(CanalOrigen.EXTERNO, identificadorExterno,
                TipoEventoCanal.CREACION_RESERVA, datos, LocalDateTime.now(reloj));
    }

    /** Cancelación enviada por el canal para una reserva que él mismo creó. */
    public ResultadoEventoCanal cancelar(String identificadorExterno) {
        return conciliacionService.procesarMensajeExterno(CanalOrigen.EXTERNO, identificadorExterno,
                TipoEventoCanal.CANCELACION_RESERVA, null, LocalDateTime.now(reloj));
    }
}
