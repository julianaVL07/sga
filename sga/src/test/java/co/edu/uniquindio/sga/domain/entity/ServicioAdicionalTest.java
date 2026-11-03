package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: L-17 (solo un servicio activo y con valor genera cargo), implementada en
 * ServicioAdicional.generarCargoPara. El folio se abre con el alojamiento y el depósito de garantía (2 cargos).
 */
class ServicioAdicionalTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 5);

    private final Folio folio = Folio.abrir("FOL-RES-2026-00001", CodigoReserva.generar(2026, 1),
            new Cargo(TipoCargo.ALOJAMIENTO, "Alojamiento 2 noches", Dinero.de(620_000), HOY),
            DepositoGarantia.de("Depósito de garantía", Dinero.de(250_000)), HOY);

    @Test
    @DisplayName("L-17: un servicio activo y con valor genera su cargo en el folio")
    void generarCargoDeServicioActivoConValor() {
        ServicioAdicional recepcion = ServicioAdicional.crearConCargo("SRV-1", "Recepción nocturna", null,
                Dinero.de(70_000));
        int cargosAntes = folio.getCargos().size();

        Cargo cargo = recepcion.generarCargoPara(folio, HOY);

        assertEquals(TipoCargo.SERVICIO_ADICIONAL, cargo.tipo());
        assertEquals("Recepción nocturna", cargo.concepto());
        assertEquals(Dinero.de(70_000), cargo.valor());
        assertEquals(cargosAntes + 1, folio.getCargos().size());
        assertTrue(folio.getCargos().contains(cargo));
    }

    @Test
    @DisplayName("L-17: un servicio inactivo no genera cargo")
    void rechazarCargoDeServicioInactivo() {
        ServicioAdicional recepcion = ServicioAdicional.crearConCargo("SRV-1", "Recepción nocturna", null,
                Dinero.de(70_000));
        recepcion.desactivar();
        int cargosAntes = folio.getCargos().size();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> recepcion.generarCargoPara(folio, HOY));

        assertTrue(error.getMessage().startsWith("L-17"));
        assertTrue(error.getMessage().contains("no está activo"));
        assertEquals(cargosAntes, folio.getCargos().size());
    }

    @Test
    @DisplayName("L-17: un servicio sin cargo (gratuito) no genera cargo")
    void rechazarCargoDeServicioSinCargo() {
        ServicioAdicional wifi = ServicioAdicional.crearSinCargo("SRV-2", "Wifi", null);
        int cargosAntes = folio.getCargos().size();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> wifi.generarCargoPara(folio, HOY));

        assertTrue(error.getMessage().startsWith("L-17"));
        assertTrue(error.getMessage().contains("no genera cargo"));
        assertEquals(cargosAntes, folio.getCargos().size());
    }
}
