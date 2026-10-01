package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import co.edu.uniquindio.sga.domain.valueobject.Titular;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Crea una reserva PENDIENTE y abre su folio (7.5, 7.7).
 *
 * Reglas: las condiciones de creación 1 a 6 (RN-01 a RN-04, RN-07, RN-20, RP-01 por medio de
 * DisponibilidadApartamentoService y de Reserva.crear), RN-05 (la cotización debe coincidir con las
 * tarifas vigentes), RN-19 (un identificador externo no se repite), RN-22 (cotización y política quedan
 * congeladas), RP-02 (depósito de garantía al abrir el folio) y RP-03 (recargo por llegada nocturna).
 */
public class CreacionReservaService {

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final AlojamientoRepository alojamientoRepository;
    private final ServicioAdicionalRepository servicioAdicionalRepository;
    private final DisponibilidadApartamentoService disponibilidadService;
    private final CotizadorEstanciaService cotizadorService;
    private final DepositoGarantia depositoGarantia;
    private final String idServicioRecepcionNocturna;

    /**
     * @param depositoGarantia            RP-02, configurable ($250.000 en el Anexo A).
     * @param idServicioRecepcionNocturna id del ServicioAdicional que cobra el recargo de RP-03 ($70.000).
     */
    public CreacionReservaService(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                  ApartamentoRepository apartamentoRepository,
                                  AlojamientoRepository alojamientoRepository,
                                  ServicioAdicionalRepository servicioAdicionalRepository,
                                  DisponibilidadApartamentoService disponibilidadService,
                                  CotizadorEstanciaService cotizadorService,
                                  DepositoGarantia depositoGarantia, String idServicioRecepcionNocturna) {
        this.reservaRepository = Objects.requireNonNull(reservaRepository);
        this.folioRepository = Objects.requireNonNull(folioRepository);
        this.apartamentoRepository = Objects.requireNonNull(apartamentoRepository);
        this.alojamientoRepository = Objects.requireNonNull(alojamientoRepository);
        this.servicioAdicionalRepository = Objects.requireNonNull(servicioAdicionalRepository);
        this.disponibilidadService = Objects.requireNonNull(disponibilidadService);
        this.cotizadorService = Objects.requireNonNull(cotizadorService);
        if (depositoGarantia == null) {
            throw new ReglaDominioException("RP-02: El depósito de garantía debe estar configurado");
        }
        this.depositoGarantia = depositoGarantia;
        this.idServicioRecepcionNocturna = idServicioRecepcionNocturna;
    }

    /**
     * @param cotizacion           la que se le mostró al titular; puede ser null y entonces se calcula aquí.
     * @param identificadorExterno obligatorio solo para el canal EXTERNO (RN-19).
     * @param ahora                fecha y hora de Colombia (3.1); el plazo de confirmación se mide desde aquí.
     */
    public Reserva registrarNuevaReserva(IdentificacionApartamento aptoId, Estancia estancia, Titular titular,
                                         List<Ocupante> ocupantes, LocalTime horaLlegada, Cotizacion cotizacion,
                                         PoliticaCancelacion politica, CanalOrigen canal,
                                         String identificadorExterno, LocalDateTime ahora) {
        requerido(aptoId, "El apartamento es obligatorio");
        requerido(ahora, "La fecha actual es obligatoria");
        Alojamiento alojamiento = alojamientoRepository.obtener()
                .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(aptoId)
                .orElseThrow(() -> new ReglaDominioException("No existe el apartamento " + aptoId.codigo()));

        if (canal == CanalOrigen.EXTERNO && identificadorExterno != null
                && reservaRepository.buscarPorCanalEIdentificadorExterno(canal, identificadorExterno.trim()).isPresent()) {
            throw new ReglaDominioException("RN-19: Ya existe una reserva del canal externo con el identificador "
                    + identificadorExterno);
        }

        int totalOcupantes = ocupantes == null ? 0 : ocupantes.size();
        disponibilidadService.validarDisponibilidad(apartamento, estancia, totalOcupantes, null);

        Cotizacion vigente = cotizadorService.cotizar(apartamento, estancia, ocupantes,
                alojamiento.getUmbralEdadFacturable());
        if (cotizacion != null && !cotizacion.valorTotal().equals(vigente.valorTotal())) {
            throw new ReglaDominioException("RN-05: La cotización recibida (" + cotizacion.valorTotal().monto()
                    + ") no coincide con las tarifas vigentes (" + vigente.valorTotal().monto() + ")");
        }

        Reserva reserva = Reserva.crear(siguienteCodigo(ahora.getYear()), apartamento, estancia, titular, ocupantes, canal,
                identificadorExterno, horaLlegada, vigente, politica, alojamiento.getUmbralEdadFacturable(), ahora);

        LocalDate hoy = ahora.toLocalDate();
        Cargo cargoAlojamiento = new Cargo(TipoCargo.ALOJAMIENTO,
                "Alojamiento " + estancia.noches() + " noches en " + aptoId.codigo(), vigente.valorTotal(), hoy);
        Folio folio = Folio.abrir("FOL-" + reserva.getCodigo().valor(), reserva.getCodigo(), cargoAlojamiento,
                depositoGarantia, hoy);

        if (horaLlegada != null && alojamiento.esLlegadaNocturna(horaLlegada)) {
            servicioRecepcionNocturna(alojamiento).generarCargoPara(folio, hoy);
        }

        reservaRepository.guardar(reserva);
        folioRepository.guardar(folio);
        return reserva;
    }

    /** Glosario: RES-YYYY-NNNNN, con secuencia incremental dentro del año de creación. */
    private CodigoReserva siguienteCodigo(int anio) {
        int ultima = reservaRepository.listarTodas().stream()
                .map(Reserva::getCodigo)
                .filter(codigo -> codigo.anio() == anio)
                .mapToInt(CodigoReserva::secuencia)
                .max()
                .orElse(0);
        return CodigoReserva.generar(anio, ultima + 1);
    }

    /** RP-03: el recargo nocturno se cobra con el servicio adicional configurado para ello. */
    private ServicioAdicional servicioRecepcionNocturna(Alojamiento alojamiento) {
        if (idServicioRecepcionNocturna == null) {
            throw new ReglaDominioException("RP-03: No está configurado el servicio de recepción nocturna");
        }
        return alojamiento.buscarServicioAdicional(idServicioRecepcionNocturna)
                .or(() -> servicioAdicionalRepository.buscarPorId(idServicioRecepcionNocturna))
                .orElseThrow(() -> new ReglaDominioException("RP-03: No existe el servicio de recepción nocturna "
                        + idServicioRecepcionNocturna));
    }

    private static void requerido(Object valor, String mensaje) {
        if (valor == null) throw new ReglaDominioException(mensaje);
    }
}
