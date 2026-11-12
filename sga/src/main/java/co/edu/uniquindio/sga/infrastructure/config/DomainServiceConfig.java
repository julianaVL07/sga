package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.port.CredencialesCanal;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.service.CanalVentaService;
import co.edu.uniquindio.sga.domain.repository.ConflictoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.service.AdministracionApartamentoService;
import co.edu.uniquindio.sga.domain.service.CalendarioOcupacionService;
import co.edu.uniquindio.sga.domain.service.CancelarReservaService;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.service.CotizadorEstanciaService;
import co.edu.uniquindio.sga.domain.service.CreacionReservaService;
import co.edu.uniquindio.sga.domain.service.CreacionTemporadaService;
import co.edu.uniquindio.sga.domain.service.DeclararNoShowService;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.service.EntregaApartamentoService;
import co.edu.uniquindio.sga.domain.service.LiquidacionSalidaService;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService;
import co.edu.uniquindio.sga.domain.service.PoliticaCancelacionService;
import co.edu.uniquindio.sga.domain.service.RegistrarBloqueoService;
import co.edu.uniquindio.sga.domain.service.ReportesService;
import co.edu.uniquindio.sga.domain.service.RetiroApartamentoService;
import co.edu.uniquindio.sga.domain.service.VencimientoReservasService;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Construye los 19 servicios de dominio del SGA como beans de Spring (Guía 09). Los servicios son Java puro y no
 * llevan @Service: la infraestructura los crea con new y el dominio nunca se entera de que existe Spring.
 * Spring resuelve el orden solo: si un servicio pide otro como parámetro, crea primero ese.
 */
@Configuration
@EnableConfigurationProperties(PoliticaCancelacionProperties.class)
public class DomainServiceConfig {

    /** Toda fecha y hora del SGA es la de Colombia (America/Bogota). Un solo reloj para todos los casos de uso. */
    @Bean
    public Clock reloj(@Value("${sga.alojamiento.zona-horaria}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    /** RP-02: depósito de garantía que se asienta en el folio de toda reserva nueva del Apartahotel Coral. */
    @Bean
    public DepositoGarantia depositoGarantia(
            @Value("${sga.alojamiento.deposito-garantia-concepto}") String concepto,
            @Value("${sga.alojamiento.deposito-garantia}") long pesos) {
        return DepositoGarantia.de(concepto, Dinero.de(pesos));
    }

    /**
     * CU-30 y RN-22: política de cancelación versionada. La vigente se lee del repositorio en cada reserva nueva;
     * la versión 1 sale de sga.politica-cancelacion.* y la guarda DatosInicialesPoliticaCancelacionConfig al
     * arrancar si aún no hay ninguna.
     */
    @Bean
    public PoliticaCancelacionService politicaCancelacionService(PoliticaCancelacionRepository politicas) {
        return new PoliticaCancelacionService(politicas);
    }

    /** CU-29 (7.4): crea la temporada y asigna la tarifa de cada apartamento activo en una sola operación. */
    @Bean
    public CreacionTemporadaService creacionTemporadaService(AlojamientoRepository alojamientos,
                                                             ApartamentoRepository apartamentos) {
        return new CreacionTemporadaService(alojamientos, apartamentos);
    }

    /** 7.5: recibe el cotizador para estimar el valor de la estancia y filtrar por precio. */
    @Bean
    public DisponibilidadApartamentoService disponibilidadApartamentoService(
            ReservaRepository reservas, ApartamentoRepository apartamentos,
            BloqueoRepository bloqueos, AlojamientoRepository alojamientos, CotizadorEstanciaService cotizador) {
        return new DisponibilidadApartamentoService(reservas, apartamentos, bloqueos, alojamientos, cotizador);
    }

    @Bean
    public CotizadorEstanciaService cotizadorEstanciaService(ApartamentoRepository apartamentos,
                                                             AlojamientoRepository alojamientos) {
        return new CotizadorEstanciaService(apartamentos, alojamientos);
    }

    /** RP-02 y RP-03: recibe el depósito de garantía y el id del servicio de recepción nocturna. */
    @Bean
    public CreacionReservaService creacionReservaService(
            ReservaRepository reservas, FolioRepository folios, ApartamentoRepository apartamentos,
            AlojamientoRepository alojamientos, ServicioAdicionalRepository servicios,
            DisponibilidadApartamentoService disponibilidad, CotizadorEstanciaService cotizador,
            DepositoGarantia deposito,
            @Value("${sga.alojamiento.id-servicio-recepcion-nocturna}") String idRecepcionNocturna) {
        return new CreacionReservaService(reservas, folios, apartamentos, alojamientos, servicios,
                disponibilidad, cotizador, deposito, idRecepcionNocturna);
    }

    @Bean
    public ConfirmacionReservaService confirmacionReservaService(ReservaRepository reservas, FolioRepository folios,
                                                                 AlojamientoRepository alojamientos) {
        return new ConfirmacionReservaService(reservas, folios, alojamientos);
    }

    @Bean
    public CancelarReservaService cancelarReservaService(ReservaRepository reservas, FolioRepository folios) {
        return new CancelarReservaService(reservas, folios);
    }

    @Bean
    public DeclararNoShowService declararNoShowService(ReservaRepository reservas, FolioRepository folios,
                                                       AlojamientoRepository alojamientos) {
        return new DeclararNoShowService(reservas, folios, alojamientos);
    }

    @Bean
    public ModificacionReservaService modificacionReservaService(
            ReservaRepository reservas, FolioRepository folios, ApartamentoRepository apartamentos,
            AlojamientoRepository alojamientos, DisponibilidadApartamentoService disponibilidad,
            CotizadorEstanciaService cotizador) {
        return new ModificacionReservaService(reservas, folios, apartamentos, alojamientos, disponibilidad, cotizador);
    }

    @Bean
    public EntregaApartamentoService entregaApartamentoService(ReservaRepository reservas,
                                                               ApartamentoRepository apartamentos) {
        return new EntregaApartamentoService(reservas, apartamentos);
    }

    @Bean
    public LiquidacionSalidaService liquidacionSalidaService(ReservaRepository reservas,
                                                             ApartamentoRepository apartamentos,
                                                             FolioRepository folios) {
        return new LiquidacionSalidaService(reservas, apartamentos, folios);
    }

    @Bean
    public RegistrarBloqueoService registrarBloqueoService(BloqueoRepository bloqueos,
                                                           ApartamentoRepository apartamentos,
                                                           ReservaRepository reservas) {
        return new RegistrarBloqueoService(bloqueos, apartamentos, reservas);
    }

    /** CU-21 (7.3): retiro de un apartamento de la venta, con eliminación lógica (12.4). */
    @Bean
    public RetiroApartamentoService retiroApartamentoService(ApartamentoRepository apartamentos,
                                                             ReservaRepository reservas) {
        return new RetiroApartamentoService(apartamentos, reservas);
    }

    /** CU-22 a CU-25 (7.3, 7.4): administración de apartamentos, tarifas con histórico y activación. */
    @Bean
    public AdministracionApartamentoService administracionApartamentoService(ApartamentoRepository apartamentos,
                                                                             AlojamientoRepository alojamientos,
                                                                             ReservaRepository reservas) {
        return new AdministracionApartamentoService(apartamentos, alojamientos, reservas);
    }

    /** CU-27 (7.1): calendario de ocupación noche por noche de un apartamento. */
    @Bean
    public CalendarioOcupacionService calendarioOcupacionService(ReservaRepository reservas,
                                                                 BloqueoRepository bloqueos) {
        return new CalendarioOcupacionService(reservas, bloqueos);
    }

    /** 7.9: reportes de ocupación, ingresos y reservas por canal del administrador. */
    @Bean
    public ReportesService reportesService(ReservaRepository reservas, FolioRepository folios,
                                           ApartamentoRepository apartamentos) {
        return new ReportesService(reservas, folios, apartamentos);
    }

    @Bean
    public VencimientoReservasService vencimientoReservasService(ReservaRepository reservas, FolioRepository folios,
                                                                 AlojamientoRepository alojamientos) {
        return new VencimientoReservasService(reservas, folios, alojamientos);
    }

    /** RN-19 y RN-22: las reservas que llegan por el canal externo también congelan la política vigente. */
    @Bean
    public ConciliacionCanalExternoService conciliacionCanalExternoService(
            ReservaRepository reservas, ApartamentoRepository apartamentos,
            EventoCanalRepository eventos, ConflictoCanalRepository conflictos,
            DisponibilidadApartamentoService disponibilidad, CreacionReservaService creacion,
            CancelarReservaService cancelacion, PoliticaCancelacionService politicas) {
        return new ConciliacionCanalExternoService(reservas, apartamentos, eventos, conflictos,
                disponibilidad, creacion, cancelacion, politicas);
    }

    /** 10.3.1: canales de venta y su credencial; los intentos rechazados van a la bitácora (7.8). */
    @Bean
    public CanalVentaService canalVentaService(CanalVentaRepository canales, EventoCanalRepository eventos,
                                               CredencialesCanal credenciales) {
        return new CanalVentaService(canales, eventos, credenciales);
    }
}