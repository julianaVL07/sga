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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-15 (Folio.registrarPago, Folio.saldo) y RN-17 (Folio.cerrar).
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
}
