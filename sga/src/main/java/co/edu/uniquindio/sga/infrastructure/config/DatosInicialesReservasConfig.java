package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.CancelarReservaService;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.service.CotizadorEstanciaService;
import co.edu.uniquindio.sga.domain.service.CreacionReservaService;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.MedioPago;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import co.edu.uniquindio.sga.domain.valueobject.Titular;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos de prueba de reservas (12.3): al menos una reserva en cada estado del ciclo de vida (sección 8) y al menos
 * una por cada canal de origen. Corre después de los datos del alojamiento (@Order(2)) y no hace nada si ya hay
 * reservas. Las fechas son relativas a hoy para que la demostración funcione cualquier día.
 *
 * Solo usa APT-101, APT-102 y APT-201 (APT-202 queda fuera de servicio y APT-301 tiene un bloqueo) y ninguna
 * estancia retenida se solapa con otra del mismo apartamento (RN-01, RN-20):
 *   APT-101  FINALIZADA  hoy-10 → hoy-7   DIRECTO   (reconstruida: estancia pasada)
 *            PENDIENTE   hoy+10 → hoy+12  PORTAL
 *            CANCELADA   hoy+20 → hoy+22  PORTAL
 *   APT-102  NO_SHOW     hoy-5  → hoy-3   PORTAL    (reconstruida: estancia pasada)
 *            CONFIRMADA  hoy+12 → hoy+15  DIRECTO
 *   APT-201  EN_CURSO    hoy-1  → hoy+2   DIRECTO   (reconstruida: entrada ayer; deja el apartamento OCUPADO)
 *            PENDIENTE   hoy+15 → hoy+18  EXTERNO   (con identificador externo, RN-19)
 *
 * Las reservas futuras nacen con CreacionReservaService (mismas reglas que CU-03, con su folio, F-08) y avanzan
 * con ConfirmacionReservaService y CancelarReservaService. Las de fechas pasadas no pueden crearse con
 * Reserva.crear (RN-04), así que parten de Reserva.reconstruir en CONFIRMADA, igual que el mapper de persistencia,
 * y desde ahí avanzan con los métodos de negocio del agregado (registrarLlegada, registrarSalida, declararNoShow).
 */
@Configuration
public class DatosInicialesReservasConfig {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesReservasConfig.class);

    private static final LocalTime HORA_LLEGADA = LocalTime.of(16, 0);

    /**
     * @param cargarDatosCoral el runner del alojamiento. Se invoca primero porque sin alojamiento, temporadas y
     *                         tarifas no hay reservas posibles (F-13); es idempotente, así que cuando ya corrió
     *                         (@Order(1)) la llamada no hace nada.
     */
    @Bean
    @Order(2)
    public CommandLineRunner cargarReservasDePrueba(
            @Qualifier("cargarDatosCoral") CommandLineRunner cargarDatosCoral,
            ReservaRepository reservaRepository,
            FolioRepository folioRepository,
            ApartamentoRepository apartamentoRepository,
            AlojamientoRepository alojamientoRepository,
            CreacionReservaService creacionReservaService,
            ConfirmacionReservaService confirmacionReservaService,
            CancelarReservaService cancelarReservaService,
            CotizadorEstanciaService cotizadorEstanciaService,
            PoliticaCancelacion politicaVigente,
            DepositoGarantia depositoGarantia,
            Clock reloj) {
        return args -> {
            if (!reservaRepository.listarTodas().isEmpty()) {
                return;                                          // no duplicar si ya hay reservas
            }
            cargarDatosCoral.run(args);
            Alojamiento coral = alojamientoRepository.obtener()
                    .orElseThrow(() -> new ReglaDominioException("F-13: No hay un alojamiento configurado"));

            Cargador cargador = new Cargador(coral, reservaRepository, folioRepository, apartamentoRepository,
                    creacionReservaService, confirmacionReservaService, cancelarReservaService,
                    cotizadorEstanciaService, politicaVigente, depositoGarantia, LocalDateTime.now(reloj));
            cargador.cargar();
            log.info("Datos de prueba de reservas cargados: {} reservas", reservaRepository.listarTodas().size());
        };
    }

    /** Agrupa las dependencias para no repetirlas en cada reserva de prueba. */
    private record Cargador(Alojamiento coral, ReservaRepository reservas, FolioRepository folios,
                            ApartamentoRepository apartamentos, CreacionReservaService creacion,
                            ConfirmacionReservaService confirmacion, CancelarReservaService cancelacion,
                            CotizadorEstanciaService cotizador, PoliticaCancelacion politicaVigente,
                            DepositoGarantia depositoGarantia, LocalDateTime ahora) {

        void cargar() {
            LocalDate hoy = ahora.toLocalDate();

            // ---------- Estancias pasadas: parten de una reserva CONFIRMADA reconstruida ----------

            // FINALIZADA: llegó, pagó todo el folio y salió; el folio queda cerrado (RN-17).
            ReservaConFolio finalizada = confirmadaEnElPasado("APT-101", hoy.minusDays(10), hoy.minusDays(7),
                    titular("Carlos Restrepo", "CC", "1094111222", "carlos.restrepo@correo.co", "3105550101"),
                    CanalOrigen.DIRECTO);
            Reserva reserva = finalizada.reserva();
            Folio folio = finalizada.folio();
            // El apartamento estaba PREPARADO al llegar (RN-11); su estado de hoy ya no depende de esta estancia.
            reserva.registrarLlegada(reserva.getEstancia().fechaEntrada(), EstadoOperativo.PREPARADO);
            LocalDate salida = reserva.getEstancia().fechaSalida();
            folio.registrarPago(new Pago(folio.saldo(), MedioPago.TARJETA_DEBITO_CREDITO, salida));
            reserva.registrarSalida(salida);
            folio.cerrar(salida);
            guardar(reserva, folio);

            // NO_SHOW: confirmada, pero el grupo no llegó antes de la hora límite (L-15); retiene el 50 % (RN-13).
            ReservaConFolio noShow = confirmadaEnElPasado("APT-102", hoy.minusDays(5), hoy.minusDays(3),
                    titular("Valentina Ríos", "CC", "1094333444", "valentina.rios@correo.co", "3115550202"),
                    CanalOrigen.PORTAL);
            reserva = noShow.reserva();
            folio = noShow.folio();
            LocalDateTime limite = reserva.getEstancia().fechaEntrada().atTime(coral.getHoraLimiteNoShow());
            Dinero retencion = reserva.calcularRetencionPorNoShow();
            reserva.declararNoShow(limite, coral.getHoraLimiteNoShow());
            folio.anularCargos("No-show de la reserva " + reserva.getCodigo().valor(), limite.toLocalDate());
            folio.registrarPenalidad(retencion, "Retención por no-show: "
                    + reserva.getPoliticaCancelacion().consecuenciaNoShow(), limite.toLocalDate());
            guardar(reserva, folio);

            // EN_CURSO: llegó ayer a APT-201, que pasa de PREPARADO a OCUPADO (RN-11, 7.6).
            ReservaConFolio enCurso = confirmadaEnElPasado("APT-201", hoy.minusDays(1), hoy.plusDays(2),
                    titular("Andrés Mejía", "CC", "1094555666", "andres.mejia@correo.co", "3125550303"),
                    CanalOrigen.DIRECTO);
            reserva = enCurso.reserva();
            Apartamento apt201 = apartamento(reserva.getApartamento());
            reserva.registrarLlegada(reserva.getEstancia().fechaEntrada(), apt201.getEstadoOperativo());
            apt201.recibirGrupo();
            guardar(reserva, enCurso.folio());
            apartamentos.guardar(apt201);

            // ---------- Estancias futuras: nacen PENDIENTE con CreacionReservaService (CU-03) ----------

            // PENDIENTE por el portal: aún no paga el anticipo ni se confirma.
            nueva("APT-101", hoy.plusDays(10), hoy.plusDays(12),
                    titular("Laura Gómez", "CC", "1094777888", "laura.gomez@correo.co", "3135550404"),
                    CanalOrigen.PORTAL, HORA_LLEGADA, null);

            // CONFIRMADA en recepción: paga el anticipo del 50 % (L-11) y se confirma (RN-09, RN-21).
            Reserva confirmada = nueva("APT-102", hoy.plusDays(12), hoy.plusDays(15),
                    titular("Santiago Herrera", "CC", "1094999000", "santiago.herrera@correo.co", "3145550505"),
                    CanalOrigen.DIRECTO, HORA_LLEGADA, null);
            Folio folioConfirmada = folios.buscarPorReserva(confirmada.getCodigo()).orElseThrow();
            folioConfirmada.registrarPago(new Pago(anticipoDe(confirmada), MedioPago.TRANSFERENCIA_BANCARIA, hoy));
            folios.guardar(folioConfirmada);
            confirmacion.confirmar(confirmada.getCodigo(), ahora);

            // CANCELADA con más de 15 días de antelación: retención 0 % (RN-13) y noches liberadas (RN-12).
            Reserva cancelada = nueva("APT-101", hoy.plusDays(20), hoy.plusDays(22),
                    titular("Mariana Castaño", "CC", "1094121314", "mariana.castano@correo.co", "3155550606"),
                    CanalOrigen.PORTAL, HORA_LLEGADA, null);
            cancelacion.cancelar(cancelada.getCodigo(), hoy, "El titular cambió sus planes de viaje");

            // EXTERNO: llega de una plataforma de terceros con su identificador (RN-19) y sin hora de llegada.
            nueva("APT-201", hoy.plusDays(15), hoy.plusDays(18),
                    titular("Emily Johnson", "PA", "PA5550123", "emily.johnson@mail.com", "+15555550707"),
                    CanalOrigen.EXTERNO, null, "BK-DEMO-000123");
        }

        private Reserva nueva(String apartamento, LocalDate entrada, LocalDate salida, Titular titular,
                              CanalOrigen canal, LocalTime horaLlegada, String identificadorExterno) {
            return creacion.registrarNuevaReserva(new IdentificacionApartamento(apartamento),
                    new Estancia(entrada, salida), titular, List.of(ocupante(titular)), horaLlegada, null,
                    politicaVigente, canal, identificadorExterno, ahora);
        }

        /**
         * Reserva CONFIRMADA de una estancia que ya empezó, con la cotización y la política vigentes congeladas
         * (RN-22) y su folio abierto como en CU-03 (cargo de alojamiento y depósito, RP-02) con el anticipo pagado
         * (L-11). Se creó 15 días antes de la entrada y se confirmó dos horas después.
         */
        private ReservaConFolio confirmadaEnElPasado(String apartamento, LocalDate entrada, LocalDate salida,
                                                     Titular titular, CanalOrigen canal) {
            Apartamento apto = apartamento(new IdentificacionApartamento(apartamento));
            Estancia estancia = new Estancia(entrada, salida);
            List<Ocupante> ocupantes = List.of(ocupante(titular));
            Cotizacion cotizacion = cotizador.cotizar(apto, estancia, ocupantes, coral.getUmbralEdadFacturable());
            LocalDateTime creacionReserva = entrada.minusDays(15).atTime(10, 0);
            CodigoReserva codigo = siguienteCodigo(creacionReserva.getYear());

            Reserva reserva = Reserva.reconstruir(codigo, apto.getIdentificacion(), estancia, titular, ocupantes,
                    canal, null, HORA_LLEGADA, cotizacion, politicaVigente, EstadoReserva.CONFIRMADA,
                    creacionReserva, creacionReserva.plusHours(2), null, null, null, null);

            LocalDate apertura = creacionReserva.toLocalDate();
            Cargo alojamiento = new Cargo(TipoCargo.ALOJAMIENTO,
                    "Alojamiento " + estancia.noches() + " noches en " + apartamento, cotizacion.valorTotal(), apertura);
            Folio folio = Folio.abrir("FOL-" + codigo.valor(), codigo, alojamiento, depositoGarantia, apertura);
            folio.registrarPago(new Pago(anticipoDe(reserva), MedioPago.TRANSFERENCIA_BANCARIA, apertura));
            return new ReservaConFolio(reserva, folio);
        }

        private Dinero anticipoDe(Reserva reserva) {
            return reserva.valorTotal().porcentaje(coral.getPorcentajeAnticipo());
        }

        /** Mismo formato y secuencia que CreacionReservaService: RES-YYYY-NNNNN. */
        private CodigoReserva siguienteCodigo(int anio) {
            int ultima = reservas.listarTodas().stream()
                    .map(Reserva::getCodigo)
                    .filter(codigo -> codigo.anio() == anio)
                    .mapToInt(CodigoReserva::secuencia)
                    .max()
                    .orElse(0);
            return CodigoReserva.generar(anio, ultima + 1);
        }

        private Apartamento apartamento(IdentificacionApartamento identificacion) {
            return apartamentos.buscarPorIdentificacion(identificacion)
                    .orElseThrow(() -> new ReglaDominioException("No existe el apartamento " + identificacion.codigo()));
        }

        private void guardar(Reserva reserva, Folio folio) {
            reservas.guardar(reserva);
            folios.guardar(folio);
        }
    }

    private record ReservaConFolio(Reserva reserva, Folio folio) {
    }

    private static Titular titular(String nombre, String tipoDocumento, String documento, String email,
                                   String telefono) {
        return new Titular(nombre, new DocumentoIdentidad(tipoDocumento, documento), email, telefono);
    }

    /** 3.2: el titular viaja y es un ocupante facturable (adulto). */
    private static Ocupante ocupante(Titular titular) {
        return new Ocupante(titular.nombre(), titular.documento(), LocalDate.of(1988, 4, 12));
    }
}
