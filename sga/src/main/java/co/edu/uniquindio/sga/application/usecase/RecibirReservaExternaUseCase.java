package co.edu.uniquindio.sga.application.usecase;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService.DatosReservaExterna;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * CU-08. GET /api/canales/{canal}/disponibilidad?pagina=, POST /api/canales/{canal}/reservas y
 * PUT /api/canales/{canal}/reservas/{identificadorExterno}/cancelar (las tres operaciones de 10.3.1).
 * Recibe los mensajes del canal externo (plataformas de terceros, 10.3.1). Un mensaje repetido no crea otra reserva (RN-19);
 * si las noches ya estaban vendidas o bloqueadas se rechaza y se registra un conflicto (RN-18). Si procede,
 * incluye CU-03 (la reserva nace PENDIENTE con su folio). Todo mensaje queda en la bitácora (7.8).
 * Actor: canal externo.
 */
@Service
public class RecibirReservaExternaUseCase {

    private final ConciliacionCanalExternoService conciliacionService;
    private final Clock reloj;
    private final int tamanoPagina;

    /** @param tamanoPagina 12.4: tamaño de página de todos los listados (L-13: configuración, nunca constante). */
    public RecibirReservaExternaUseCase(ConciliacionCanalExternoService conciliacionService, Clock reloj,
                                        @Value("${sga.paginacion.tamano}") int tamanoPagina) {
        this.conciliacionService = Objects.requireNonNull(conciliacionService);
        this.reloj = Objects.requireNonNull(reloj);
        this.tamanoPagina = tamanoPagina;
    }

    /**
     * Consulta de disponibilidad del canal por rango de fechas y composición del grupo (10.3.1, operación 1),
     * 10 resultados por página (12.4). La disponibilidad la decide el dominio y la consulta queda una sola vez en la
     * bitácora (7.8); aquí solo se pagina el resultado, como en la búsqueda de disponibles (CU-01).
     */
    @Transactional(readOnly = true)
    public Pagina<Apartamento> consultarDisponibilidad(LocalDate fechaEntrada, LocalDate fechaSalida,
                                                       int totalOcupantes, int pagina) {
        return Pagina.de(conciliacionService.consultarDisponibilidad(CanalOrigen.EXTERNO,
                new Estancia(fechaEntrada, fechaSalida), totalOcupantes, LocalDateTime.now(reloj)),
                pagina, tamanoPagina);
    }

    /** Creación de una reserva enviada por el canal. */
    @Transactional
    public ResultadoEventoCanal ejecutar(String identificadorExterno, DatosReservaExterna datos) {
        return conciliacionService.procesarMensajeExterno(CanalOrigen.EXTERNO, identificadorExterno,
                TipoEventoCanal.CREACION_RESERVA, datos, LocalDateTime.now(reloj));
    }

    /** Cancelación enviada por el canal para una reserva que él mismo creó. */
    @Transactional
    public ResultadoEventoCanal cancelar(String identificadorExterno) {
        return conciliacionService.procesarMensajeExterno(CanalOrigen.EXTERNO, identificadorExterno,
                TipoEventoCanal.CANCELACION_RESERVA, null, LocalDateTime.now(reloj));
    }
}