package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.ConflictoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.Titular;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Procesa los mensajes que llegan del canal externo (10.3.1) y deja cada uno en la bitácora (7.8).
 *
 * Reglas: RN-01 (el canal externo vende sobre el mismo inventario), RN-18 (si las noches ya no están libres
 * se rechaza y se registra un conflicto para revisión del administrador) y RN-19 (un mensaje repetido con
 * el mismo canal, identificador y tipo se ignora; nunca crea una segunda reserva).
 */
public class ConciliacionCanalExternoService {

    /**
     * Datos de la reserva que envía el canal. (El catálogo lo nombra DatosReservaExterna; no está entre los
     * objetos de valor, por eso vive aquí.) En una cancelación puede llegar null.
     */
    public record DatosReservaExterna(IdentificacionApartamento apartamento, Estancia estancia, Titular titular,
                                      List<Ocupante> ocupantes, LocalTime horaEstimadaLlegada) {
        public DatosReservaExterna {
            ocupantes = ocupantes == null ? null : List.copyOf(ocupantes);
        }
    }

    private final ReservaRepository reservaRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final EventoCanalRepository eventoCanalRepository;
    private final ConflictoCanalRepository conflictoCanalRepository;
    private final DisponibilidadApartamentoService disponibilidadService;
    private final CreacionReservaService creacionReservaService;
    private final CancelarReservaService cancelarReservaService;
    private final PoliticaCancelacion politicaVigente;

    /** @param politicaVigente política que se congela en las reservas que llegan por el canal (RN-22). */
    public ConciliacionCanalExternoService(ReservaRepository reservaRepository,
                                           ApartamentoRepository apartamentoRepository,
                                           EventoCanalRepository eventoCanalRepository,
                                           ConflictoCanalRepository conflictoCanalRepository,
                                           DisponibilidadApartamentoService disponibilidadService,
                                           CreacionReservaService creacionReservaService,
                                           CancelarReservaService cancelarReservaService,
                                           PoliticaCancelacion politicaVigente) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.eventoCanalRepository = Objects.requireNonNull(eventoCanalRepository);
        this.conflictoCanalRepository = Objects.requireNonNull(conflictoCanalRepository);
        this.disponibilidadService = Objects.requireNonNull(disponibilidadService);
        this.creacionReservaService = Objects.requireNonNull(creacionReservaService);
        this.cancelarReservaService = Objects.requireNonNull(cancelarReservaService);
        this.politicaVigente = Objects.requireNonNull(politicaVigente);
    }

    /**
     * @param refExterna identificador de la reserva en el canal (el catálogo usa ReferenciaExterna; aquí es texto).
     * @param ahora      fecha y hora de recepción del mensaje.
     */
    public ResultadoEventoCanal procesarMensajeExterno(CanalOrigen canal, String refExterna, TipoEventoCanal tipoEvento,
                                                       DatosReservaExterna datos, LocalDateTime ahora) {
        if (canal == null || tipoEvento == null || ahora == null) {
            throw new ReglaDominioException("El canal, el tipo de evento y la fecha del mensaje son obligatorios");
        }
        if (tipoEvento.requiereIdentificadorExterno() && (refExterna == null || refExterna.isBlank())) {
            throw new ReglaDominioException("RN-19: Los mensajes de creación y cancelación traen identificador externo");
        }
        String referencia = refExterna == null ? null : refExterna.trim();

        ResultadoEventoCanal resultado;
        if (canal != CanalOrigen.EXTERNO) {
            resultado = ResultadoEventoCanal.RECHAZADO_INVALIDO;
        } else if (tipoEvento.requiereIdentificadorExterno()
                && eventoCanalRepository.existeDuplicado(canal, referencia, tipoEvento)) {
            resultado = ResultadoEventoCanal.DUPLICADO_IGNORADO;
        } else {
            resultado = switch (tipoEvento) {
                case CONSULTA_DISPONIBILIDAD -> consultar(datos);
                case CREACION_RESERVA -> crear(canal, referencia, datos, ahora);
                case CANCELACION_RESERVA -> cancelar(canal, referencia, ahora);
            };
        }

        eventoCanalRepository.guardar(EventoCanal.auditar("EVT-" + UUID.randomUUID(), tipoEvento, canal, referencia,
                resultado, ahora));
        return resultado;
    }

    /** Consulta del canal (10.3.1): la lista la entrega DisponibilidadApartamentoService; aquí queda auditada. */
    public List<Apartamento> consultarDisponibilidad(CanalOrigen canal, Estancia estancia, int totalOcupantes,
                                                     LocalDateTime ahora) {
        List<Apartamento> disponibles = disponibilidadService.buscarDisponibles(estancia, totalOcupantes);
        eventoCanalRepository.guardar(EventoCanal.auditar("EVT-" + UUID.randomUUID(),
                TipoEventoCanal.CONSULTA_DISPONIBILIDAD, canal, null, ResultadoEventoCanal.ACEPTADO, ahora));
        return disponibles;
    }

    private ResultadoEventoCanal consultar(DatosReservaExterna datos) {
        return datos == null || datos.estancia() == null
                ? ResultadoEventoCanal.RECHAZADO_INVALIDO
                : ResultadoEventoCanal.ACEPTADO;
    }

    private ResultadoEventoCanal crear(CanalOrigen canal, String referencia, DatosReservaExterna datos,
                                       LocalDateTime ahora) {
        if (datos == null || datos.apartamento() == null || datos.estancia() == null || datos.titular() == null
                || datos.ocupantes() == null || datos.ocupantes().isEmpty()) {
            return ResultadoEventoCanal.RECHAZADO_INVALIDO;
        }
        // RN-19: si la reserva ya existe (por ejemplo, aceptada antes de un reintento) no se crea otra.
        if (reservaRepository.buscarPorCanalEIdentificadorExterno(canal, referencia).isPresent()) {
            return ResultadoEventoCanal.DUPLICADO_IGNORADO;
        }
        Optional<Apartamento> apartamento = apartamentoRepository.buscarPorIdentificacion(datos.apartamento());
        if (apartamento.isEmpty()) {
            return ResultadoEventoCanal.RECHAZADO_INVALIDO;
        }

        // RN-18: las noches ya vendidas por otro canal (o bloqueadas) son un conflicto, no un dato inválido.
        Optional<String> conflicto = disponibilidadService.conflictoDeInventario(apartamento.get(), datos.estancia(), null);
        if (conflicto.isPresent()) {
            conflictoCanalRepository.guardar(ConflictoCanal.registrar("CNF-" + UUID.randomUUID(), canal, referencia,
                    conflicto.get(), ahora.toLocalDate()));
            return ResultadoEventoCanal.RECHAZADO_CONFLICTO;
        }

        try {
            creacionReservaService.registrarNuevaReserva(datos.apartamento(), datos.estancia(), datos.titular(),
                    datos.ocupantes(), datos.horaEstimadaLlegada(), null, politicaVigente, canal, referencia, ahora);
            return ResultadoEventoCanal.ACEPTADO;
        } catch (ReglaDominioException e) {
            // Capacidad, estancia mínima, fechas pasadas, titular no facturable, etc.
            return ResultadoEventoCanal.RECHAZADO_INVALIDO;
        }
    }

    private ResultadoEventoCanal cancelar(CanalOrigen canal, String referencia, LocalDateTime ahora) {
        Optional<Reserva> reserva = reservaRepository.buscarPorCanalEIdentificadorExterno(canal, referencia);
        if (reserva.isEmpty()) {
            return ResultadoEventoCanal.RECHAZADO_INVALIDO;
        }
        try {
            cancelarReservaService.cancelar(reserva.get().getCodigo(), ahora.toLocalDate(),
                    "Cancelación recibida del canal externo (" + referencia + ")");
            return ResultadoEventoCanal.ACEPTADO;
        } catch (ReglaDominioException e) {
            // Por ejemplo, la reserva ya había iniciado o ya estaba cancelada (RN-08).
            return ResultadoEventoCanal.RECHAZADO_INVALIDO;
        }
    }
}
