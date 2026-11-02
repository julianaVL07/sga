package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.MedioPago;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-15 (Folio.registrarPago, Folio.saldo), RN-16 (Folio.registrarMovimientoInverso y listas
 * de solo lectura), RN-17 (Folio.cerrar) y RP-02 (Folio.abrir con DepositoGarantia.generarCargo).
 * El folio se abre con el alojamiento ($620.000) y el depósito de garantía de RP-02 ($250.000).
 */
class FolioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private Folio folio;

    @BeforeEach
    void abrirFolio() {
        Cargo alojamiento = new Cargo(TipoCargo.ALOJAMIENTO, "Alojamiento 2 noches", Dinero.de(620_000), HOY);
        folio = Folio.abrir("FOL-RES-2026-00001", CodigoReserva.generar(2026, 1), alojamiento,
                DepositoGarantia.de("Depósito de garantía", Dinero.de(250_000)), HOY);
    }

    @Test
    @DisplayName("RN-15: el pago queda con medio y fecha, y el saldo es cargos menos pagos")
    void registrarPagoValidoYCalcularSaldoCorrectamente() {
        folio.registrarPago(new Pago(Dinero.de(435_000), MedioPago.TRANSFERENCIA_BANCARIA, HOY));

        Pago registrado = folio.getPagos().get(0);
        assertEquals(MedioPago.TRANSFERENCIA_BANCARIA, registrado.medio());
        assertEquals(HOY, registrado.fecha());
        assertEquals(Dinero.de(870_000), folio.totalCargos());
        assertEquals(Dinero.de(435_000), folio.saldo());

        ReglaDominioException sinMedio = assertThrows(ReglaDominioException.class,
                () -> folio.registrarPago(new Pago(Dinero.de(1_000), null, HOY)));
        assertTrue(sinMedio.getMessage().startsWith("RN-15"));
    }

    @Test
    @DisplayName("RN-17: con saldo pendiente el folio no se cierra sin autorización")
    void rechazarCierreConSaldoPendienteSinAutorizacion() {
        folio.registrarPago(new Pago(Dinero.de(500_000), MedioPago.TARJETA_DEBITO_CREDITO, HOY));

        ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> folio.cerrar(HOY));

        assertTrue(error.getMessage().startsWith("RN-17"));
        assertFalse(folio.isCerrado());
    }

    // ---------------------------------------------------------------- RN-16

    @Test
    @DisplayName("RN-16: una corrección asienta el cargo contrario sin tocar el original y el saldo queda corregido")
    void registrarMovimientoInversoCorrigeElSaldo() {
        Cargo alojamiento = folio.getCargos().get(0);

        folio.registrarMovimientoInverso(alojamiento, "Tarifa mal digitada", HOY.plusDays(1));

        List<Cargo> cargos = folio.getCargos();
        assertEquals(3, cargos.size());
        assertSame(alojamiento, cargos.get(0));                       // el original no se edita ni se borra
        Cargo inverso = cargos.get(2);
        assertEquals(TipoCargo.ALOJAMIENTO, inverso.tipo());
        assertEquals(Dinero.de(620_000).negar(), inverso.valor());
        assertEquals(HOY.plusDays(1), inverso.fecha());
        assertTrue(inverso.concepto().contains("Tarifa mal digitada"));
        assertEquals(Dinero.de(250_000), folio.saldo());              // solo queda el depósito
    }

    @Test
    @DisplayName("RN-16: no hay forma de editar ni borrar cargos o pagos: las listas expuestas son inmutables")
    void cargosYPagosNoSeEditanNiSeBorran() {
        folio.registrarPago(new Pago(Dinero.de(100_000), MedioPago.TARJETA_DEBITO_CREDITO, HOY));
        Cargo ajeno = new Cargo(TipoCargo.SERVICIO_ADICIONAL, "Cena en balcón", Dinero.de(260_000), HOY);

        assertThrows(UnsupportedOperationException.class, () -> folio.getCargos().remove(0));
        assertThrows(UnsupportedOperationException.class, () -> folio.getCargos().set(0, ajeno));
        assertThrows(UnsupportedOperationException.class, () -> folio.getCargos().add(ajeno));
        assertThrows(UnsupportedOperationException.class, () -> folio.getPagos().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> folio.getPagos().set(0, new Pago(Dinero.de(1), MedioPago.TARJETA_DEBITO_CREDITO, HOY)));

        assertEquals(2, folio.getCargos().size());
        assertEquals(Dinero.de(100_000), folio.totalPagado());
    }

    @Test
    @DisplayName("RN-16: solo se corrigen cargos del propio folio y un folio cerrado no admite correcciones")
    void rechazarCorreccionInvalida() {
        Cargo ajeno = new Cargo(TipoCargo.SERVICIO_ADICIONAL, "Cena en balcón", Dinero.de(260_000), HOY);
        ReglaDominioException deOtroFolio = assertThrows(ReglaDominioException.class,
                () -> folio.registrarMovimientoInverso(ajeno, "No corresponde", HOY));
        assertTrue(deOtroFolio.getMessage().startsWith("RN-16"));

        folio.registrarPago(new Pago(folio.saldo(), MedioPago.TRANSFERENCIA_BANCARIA, HOY));
        folio.cerrar(HOY);
        ReglaDominioException cerrado = assertThrows(ReglaDominioException.class,
                () -> folio.registrarMovimientoInverso(folio.getCargos().get(0), "Tarde", HOY));
        assertTrue(cerrado.getMessage().startsWith("RN-16"));
        assertEquals(2, folio.getCargos().size());
    }

    // ---------------------------------------------------------------- RP-02

    @Test
    @DisplayName("RP-02: al abrir el folio queda el depósito de $250.000 como cargo AJUSTE_MODIFICACION y el saldo lo "
            + "incluye")
    void abrirFolioAsientaElDeposito() {
        List<Cargo> cargos = folio.getCargos();

        assertEquals(2, cargos.size());
        assertEquals(TipoCargo.ALOJAMIENTO, cargos.get(0).tipo());
        Cargo deposito = cargos.get(1);
        assertEquals(TipoCargo.AJUSTE_MODIFICACION, deposito.tipo());
        assertEquals(Dinero.de(250_000), deposito.valor());
        assertEquals("Depósito de garantía", deposito.concepto());
        assertEquals(HOY, deposito.fecha());
        assertEquals(Dinero.de(870_000), folio.saldo());
        assertFalse(folio.getDepositoGarantia().reembolsado());
    }

    @Test
    @DisplayName("RP-02: el folio no se abre sin depósito de garantía, y el depósito exige un monto mayor que cero")
    void rechazarFolioSinDeposito() {
        Cargo alojamiento = new Cargo(TipoCargo.ALOJAMIENTO, "Alojamiento 2 noches", Dinero.de(620_000), HOY);

        ReglaDominioException sinDeposito = assertThrows(ReglaDominioException.class,
                () -> Folio.abrir("FOL-RES-2026-00002", CodigoReserva.generar(2026, 2), alojamiento, null, HOY));
        assertTrue(sinDeposito.getMessage().startsWith("RP-02"));

        ReglaDominioException enCero = assertThrows(ReglaDominioException.class,
                () -> DepositoGarantia.de("Depósito de garantía", Dinero.cero()));
        assertTrue(enCero.getMessage().startsWith("RP-02"));
    }
}
