package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ConflictoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.service.AdministracionApartamentoService;
import co.edu.uniquindio.sga.domain.service.CancelarReservaService;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.service.CotizadorEstanciaService;
import co.edu.uniquindio.sga.domain.service.CreacionReservaService;
import co.edu.uniquindio.sga.domain.service.DeclararNoShowService;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.service.EntregaApartamentoService;
import co.edu.uniquindio.sga.domain.service.LiquidacionSalidaService;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService;
import co.edu.uniquindio.sga.domain.service.RegistrarBloqueoService;
import co.edu.uniquindio.sga.domain.service.RetiroApartamentoService;
import co.edu.uniquindio.sga.domain.service.VencimientoReservasService;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Construye los 14 servicios de dominio del SGA como beans de Spring (Guía 09). Los servicios son Java puro y no
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
     * RN-22: política de cancelación vigente que se congela en cada reserva nueva. Versión, vigencia, consecuencia
     * del no-show y tramos (A.4) salen de sga.politica-cancelacion.* (L-13: configuración, nunca constante).
     * L-10 (al menos dos tramos y uno desde 0 días) lo valida PoliticaCancelacion al construirse.
     */
    @Bean
    public PoliticaCancelacion politicaVigente(PoliticaCancelacionProperties propiedades) {
        return propiedades.aPolitica();
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
            CancelarReservaService cancelacion, PoliticaCancelacion politicaVigente) {
        return new ConciliacionCanalExternoService(reservas, apartamentos, eventos, conflictos,
                disponibilidad, creacion, cancelacion, politicaVigente);
    }
}