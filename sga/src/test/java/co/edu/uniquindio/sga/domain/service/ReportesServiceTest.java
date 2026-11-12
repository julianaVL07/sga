package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.MedioPago;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos.IngresoPorMedio;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion.OcupacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal.ReservasDeCanal;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: 7.9 (reportes de ocupación, ingresos y reservas por canal), implementada en
 * ReportesService. Usa los repositorios en memoria de EscenarioCoral con dos apartamentos activos: APT-101 y APT-102.
 * El periodo de las pruebas es del 1 al 10 de noviembre de 2026, ambas fechas incluidas: 10 noches.
 */
class ReportesServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 10);
    private static final IdentificacionApartamento APT_102 = new IdentificacionApartamento("APT-102");

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final ReportesService reportes =
            new ReportesService(escenario.reservas, escenario.folios, escenario.apartamentos);
    private int secuencia = 0;

    ReportesServiceTest() {
        Apartamento apt102 = Apartamento.crear(APT_102, "Apartamento 102", "Vista a la bahía", 1, new Capacidad(2),
                new Dotacion(List.of("Cava de vinos")), List.of(new Imagen("https://coral.co/apt-102.jpg", true)));
        apt102.definirTarifa(new Tarifa("T-BASE", Dinero.de(330_000)));
        apt102.definirTarifa(new Tarifa("T-ALTA", Dinero.de(660_000)));
        apt102.activar(escenario.alojamiento.getTemporadas());
        escenario.apartamentos.guardar(apt102);
    }

    // ==================== Ocupación ====================

    @Test
    @DisplayName("7.9: sin reservas la ocupación es 0 % sobre apartamentos activos × noches del periodo")
    void ocupacionSinReservas() {
        ReporteOcupacion reporte = reportes.ocupacion(DESDE, HASTA);

        assertEquals(10, reporte.nochesPeriodo());
        assertEquals(2, reporte.apartamentosActivos());
        assertEquals(20, reporte.nochesDisponibles());
        assertEquals(0, reporte.nochesVendidas());
        assertEquals(new BigDecimal("0.00"), reporte.porcentajeOcupacion());
        assertEquals(List.of("APT-101", "APT-102"),
                reporte.apartamentos().stream().map(a -> a.apartamento().codigo()).toList());
    }

    @Test
    @DisplayName("7.9: cuentan las noches CONFIRMADA, EN_CURSO y FINALIZADA; PENDIENTE, CANCELADA y NO_SHOW no")
    void ocupacionCuentaSoloNochesVendidas() {
        reserva(EscenarioCoral.APT_101, dia(2), dia(5), EstadoReserva.CONFIRMADA, CanalOrigen.PORTAL);   // 3
        reserva(EscenarioCoral.APT_101, dia(5), dia(7), EstadoReserva.FINALIZADA, CanalOrigen.DIRECTO);  // 2
        reserva(APT_102, dia(1), dia(2), EstadoReserva.EN_CURSO, CanalOrigen.EXTERNO);                   // 1
        reserva(APT_102, dia(3), dia(6), EstadoReserva.PENDIENTE, CanalOrigen.PORTAL);
        reserva(APT_102, dia(6), dia(8), EstadoReserva.CANCELADA, CanalOrigen.PORTAL);
        reserva(APT_102, dia(8), dia(9), EstadoReserva.NO_SHOW, CanalOrigen.DIRECTO);

        ReporteOcupacion reporte = reportes.ocupacion(DESDE, HASTA);

        assertEquals(6, reporte.nochesVendidas());
        assertEquals(new BigDecimal("30.00"), reporte.porcentajeOcupacion());           // 6 / (2 × 10)
        OcupacionApartamento apt101 = reporte.apartamentos().get(0);
        assertEquals(5, apt101.nochesVendidas());
        assertEquals(new BigDecimal("50.00"), apt101.porcentajeOcupacion());
        assertTrue(apt101.activo());
        assertEquals(1, reporte.apartamentos().get(1).nochesVendidas());
    }

    @Test
    @DisplayName("7.9: de una estancia que empieza antes o termina después del periodo solo cuentan las noches de adentro")
    void ocupacionRecortaLasEstanciasAlPeriodo() {
        // Del 29-oct al 3-nov: dentro solo las noches del 1 y del 2
        reserva(EscenarioCoral.APT_101, LocalDate.of(2026, 10, 29), dia(3), EstadoReserva.FINALIZADA,
                CanalOrigen.PORTAL);
        // Del 9 al 14-nov: dentro las noches del 9 y del 10 (hasta va incluida)
        reserva(APT_102, dia(9), dia(14), EstadoReserva.CONFIRMADA, CanalOrigen.PORTAL);
        // Fuera del periodo: no cuenta
        reserva(APT_102, dia(20), dia(22), EstadoReserva.CONFIRMADA, CanalOrigen.PORTAL);

        ReporteOcupacion reporte = reportes.ocupacion(DESDE, HASTA);

        assertEquals(4, reporte.nochesVendidas());
        assertEquals(new BigDecimal("20.00"), reporte.porcentajeOcupacion());
    }

    @Test
    @DisplayName("7.9: un periodo de un solo día tiene una noche y el porcentaje se redondea a dos decimales")
    void ocupacionDeUnDiaYRedondeo() {
        reserva(EscenarioCoral.APT_101, dia(1), dia(2), EstadoReserva.CONFIRMADA, CanalOrigen.PORTAL);

        ReporteOcupacion unDia = reportes.ocupacion(dia(1), dia(1));
        assertEquals(1, unDia.nochesPeriodo());
        assertEquals(new BigDecimal("50.00"), unDia.porcentajeOcupacion());

        ReporteOcupacion tresDias = reportes.ocupacion(dia(1), dia(3));                  // 1 / (2 × 3)
        assertEquals(new BigDecimal("16.67"), tresDias.porcentajeOcupacion());
    }

    @Test
    @DisplayName("7.9: un apartamento que vendió noches pero ya no está a la venta sale en el detalle como inactivo")
    void ocupacionDeApartamentoRetirado() {
        reserva(new IdentificacionApartamento("APT-999"), dia(1), dia(3), EstadoReserva.FINALIZADA,
                CanalOrigen.PORTAL);

        ReporteOcupacion reporte = reportes.ocupacion(DESDE, HASTA);

        assertEquals(2, reporte.apartamentosActivos());
        assertEquals(2, reporte.nochesVendidas());
        OcupacionApartamento retirado = reporte.apartamentos().get(2);
        assertEquals("APT-999", retirado.apartamento().codigo());
        assertFalse(retirado.activo());
    }

    // ==================== Ingresos ====================

    @Test
    @DisplayName("7.9: los ingresos suman los pagos del periodo por medio de pago; los cargos no cuentan")
    void ingresosSumanSoloPagosDelPeriodo() {
        folio(List.of(
                new Pago(Dinero.de(500_000), MedioPago.TRANSFERENCIA_BANCARIA, dia(2)),
                new Pago(Dinero.de(250_000), MedioPago.TARJETA_DEBITO_CREDITO, dia(10)),
                new Pago(Dinero.de(999_000), MedioPago.TARJETA_DEBITO_CREDITO, dia(11))));        // fuera
        folio(List.of(new Pago(Dinero.de(250_000), MedioPago.TRANSFERENCIA_BANCARIA, dia(1))));
        folio(List.of(new Pago(Dinero.de(80_000), MedioPago.TRANSFERENCIA_BANCARIA,
                LocalDate.of(2026, 10, 31))));                                                     // fuera

        ReporteIngresos reporte = reportes.ingresos(DESDE, HASTA);

        assertEquals(Dinero.de(1_000_000), reporte.total());
        assertEquals(3, reporte.cantidadPagos());
        IngresoPorMedio tarjeta = medio(reporte, MedioPago.TARJETA_DEBITO_CREDITO);
        assertEquals(Dinero.de(250_000), tarjeta.total());
        assertEquals(1, tarjeta.cantidadPagos());
        assertEquals(new BigDecimal("25.00"), tarjeta.porcentaje());
        IngresoPorMedio transferencia = medio(reporte, MedioPago.TRANSFERENCIA_BANCARIA);
        assertEquals(Dinero.de(750_000), transferencia.total());
        assertEquals(2, transferencia.cantidadPagos());
        assertEquals(new BigDecimal("75.00"), transferencia.porcentaje());
    }

    @Test
    @DisplayName("7.9: sin pagos en el periodo el total es cero y salen todos los medios en cero")
    void ingresosSinPagos() {
        folio(List.of());                                                                   // solo cargos

        ReporteIngresos reporte = reportes.ingresos(DESDE, HASTA);

        assertTrue(reporte.total().esCero());
        assertEquals(0, reporte.cantidadPagos());
        assertEquals(MedioPago.values().length, reporte.medios().size());
        assertTrue(reporte.medios().stream().allMatch(m -> m.total().esCero()
                && m.porcentaje().compareTo(BigDecimal.ZERO) == 0));
    }

    // ==================== Reservas por canal ====================

    @Test
    @DisplayName("7.9: las reservas por canal cuentan las de entrada en el periodo, incluidas canceladas y no-show")
    void reservasPorCanalIncluyeCanceladasYNoShow() {
        reserva(EscenarioCoral.APT_101, dia(1), dia(3), EstadoReserva.FINALIZADA, CanalOrigen.PORTAL);
        reserva(EscenarioCoral.APT_101, dia(4), dia(6), EstadoReserva.CANCELADA, CanalOrigen.PORTAL);
        reserva(APT_102, dia(10), dia(12), EstadoReserva.CONFIRMADA, CanalOrigen.PORTAL);
        reserva(APT_102, dia(2), dia(3), EstadoReserva.NO_SHOW, CanalOrigen.DIRECTO);
        // Entrada fuera del periodo: no cuentan aunque la estancia pase por él
        reserva(APT_102, LocalDate.of(2026, 10, 30), dia(2), EstadoReserva.FINALIZADA, CanalOrigen.EXTERNO);
        reserva(APT_102, dia(11), dia(13), EstadoReserva.PENDIENTE, CanalOrigen.EXTERNO);

        ReporteReservasPorCanal reporte = reportes.reservasPorCanal(DESDE, HASTA);

        assertEquals(4, reporte.totalReservas());
        assertEquals(List.of(CanalOrigen.PORTAL, CanalOrigen.DIRECTO, CanalOrigen.EXTERNO),
                reporte.canales().stream().map(ReservasDeCanal::canal).toList());
        ReservasDeCanal portal = reporte.canales().get(0);
        assertEquals(3, portal.total());
        assertEquals(1, portal.activas());
        assertEquals(1, portal.finalizadas());
        assertEquals(1, portal.canceladas());
        assertEquals(0, portal.noShow());
        assertEquals(new BigDecimal("75.00"), portal.porcentaje());
        ReservasDeCanal directo = reporte.canales().get(1);
        assertEquals(1, directo.noShow());
        assertEquals(new BigDecimal("25.00"), directo.porcentaje());
        ReservasDeCanal externo = reporte.canales().get(2);
        assertEquals(0, externo.total());
        assertEquals(new BigDecimal("0.00"), externo.porcentaje());
    }

    // ==================== Periodo ====================

    @Test
    @DisplayName("7.9: desde posterior a hasta o una fecha vacía se rechazan en los tres reportes")
    void periodoInvalido() {
        ReglaDominioException invertido = assertThrows(ReglaDominioException.class,
                () -> reportes.ocupacion(HASTA, DESDE));
        assertTrue(invertido.getMessage().startsWith("7.9"));
        assertThrows(ReglaDominioException.class, () -> reportes.ingresos(HASTA, DESDE));
        assertThrows(ReglaDominioException.class, () -> reportes.reservasPorCanal(null, HASTA));
        assertThrows(ReglaDominioException.class, () -> reportes.ocupacion(DESDE, null));
    }

    // ==================== Apoyo ====================

    private static LocalDate dia(int diaDeNoviembre) {
        return LocalDate.of(2026, 11, diaDeNoviembre);
    }

    private void reserva(IdentificacionApartamento apartamento, LocalDate entrada, LocalDate salida,
                         EstadoReserva estado, CanalOrigen canal) {
        secuencia++;
        String idExterno = canal.exigeIdentificadorExterno() ? "EXT-" + secuencia : null;
        escenario.reservas.guardar(Reserva.reconstruir(CodigoReserva.generar(2026, secuencia), apartamento,
                new Estancia(entrada, salida), escenario.titular, List.of(escenario.ana), canal, idExterno, null,
                null, escenario.politica, estado, EscenarioCoral.AHORA.plusMinutes(secuencia), null, null, null,
                null, null));
    }

    private void folio(List<Pago> pagos) {
        secuencia++;
        Cargo alojamiento = new Cargo(TipoCargo.ALOJAMIENTO, "Alojamiento", Dinero.de(2_000_000), DESDE);
        escenario.folios.guardar(Folio.reconstruir("FOL-" + secuencia, CodigoReserva.generar(2026, secuencia), null,
                DESDE, List.of(alojamiento), pagos, false, null, null));
    }

    private static IngresoPorMedio medio(ReporteIngresos reporte, MedioPago medio) {
        return reporte.medios().stream().filter(m -> m.medio() == medio).findFirst().orElseThrow();
    }
}
