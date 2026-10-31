package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.RegistroTarifa;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-02, implementada en Apartamento.admite() y Apartamento.validarCapacidadPara();
 * 7.3 (retiro de la venta con eliminación lógica, 12.4), implementada en Apartamento.retirarDeLaVenta();
 * 7.4 (CU-24 histórico de tarifas y CU-25 activación), implementada en Apartamento.definirTarifa(tarifa, fecha),
 * Apartamento.definirTarifas() y Apartamento.activar().
 */
class ApartamentoTest {

    @Test
    @DisplayName("RN-02: un apartamento de capacidad 3 rechaza un grupo de 4 y admite uno de 3")
    void rechazarGrupoQueSuperaCapacidadMaxima() {
        Apartamento apartamento = Apartamento.crear(new IdentificacionApartamento("APT-101"), "Apartamento 101",
                "Vista al mar", 1, new Capacidad(3), new Dotacion(List.of("Aire acondicionado")),
                List.of(new Imagen("https://coral.co/apt-101.jpg", true)));

        assertFalse(apartamento.admite(4));
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> apartamento.validarCapacidadPara(4));
        assertTrue(error.getMessage().startsWith("RN-02"));

        assertTrue(apartamento.admite(3));
        assertDoesNotThrow(() -> apartamento.validarCapacidadPara(3));
    }

    @Test
    @DisplayName("7.3: sin reservas activas ni futuras el apartamento se retira de la venta (eliminación lógica)")
    void retirarDeLaVentaSinReservasPendientes() {
        Apartamento apartamento = apartamentoActivo();

        apartamento.retirarDeLaVenta(false);

        assertTrue(apartamento.isEliminado());
        assertFalse(apartamento.isActivo());
        assertFalse(apartamento.estaVigenteParaVenta());
    }

    @Test
    @DisplayName("7.3: con reservas activas o futuras no se retira y queda a la venta")
    void rechazarRetiroConReservasActivasOFuturas() {
        Apartamento apartamento = apartamentoActivo();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> apartamento.retirarDeLaVenta(true));

        assertTrue(error.getMessage().startsWith("7.3"));
        assertFalse(apartamento.isEliminado());
        assertTrue(apartamento.estaVigenteParaVenta());
    }

    @Test
    @DisplayName("7.3: un apartamento ya retirado no se retira otra vez ni puede volver a activarse")
    void rechazarRetiroDeUnApartamentoYaEliminado() {
        Apartamento apartamento = apartamentoActivo();
        apartamento.retirarDeLaVenta(false);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> apartamento.retirarDeLaVenta(false));

        assertTrue(error.getMessage().startsWith("7.3"));
        assertThrows(ReglaDominioException.class, () -> apartamento.activar(List.of(Temporada.crearBase("T-BASE", "Base"))));
    }

    // ==================== CU-24 y CU-25: histórico de tarifas y activación (7.4) ====================

    private static final LocalDate ENERO = LocalDate.of(2026, 1, 10);
    private static final LocalDate MARZO = LocalDate.of(2026, 3, 1);
    private static final Temporada BASE = Temporada.crearBase("T-BASE", "Base");
    private static final Temporada ALTA = Temporada.crear("T-ALTA", "Alta", TipoTemporada.ALTA,
            List.of(new RangoFechas(LocalDate.of(2026, 12, 10), LocalDate.of(2027, 1, 21))));

    @Test
    @DisplayName("7.4: al reemplazar una tarifa la anterior queda en el histórico con su vigencia; rige la nueva")
    void reemplazarTarifaConservaElHistorico() {
        Apartamento apartamento = apartamentoNuevo();
        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(280_000)), ENERO);
        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(300_000)), MARZO);

        assertEquals(Dinero.de(300_000), apartamento.tarifaPara(BASE).valorPorOcupante());
        assertEquals(1, apartamento.getTarifas().size());

        List<RegistroTarifa> historico = apartamento.getHistoricoTarifas();
        assertEquals(2, historico.size());
        assertEquals(new RegistroTarifa("T-BASE", Dinero.de(300_000), MARZO, null), historico.get(0));
        assertEquals(new RegistroTarifa("T-BASE", Dinero.de(280_000), ENERO, MARZO), historico.get(1));
        assertTrue(historico.get(0).estaVigente());
        assertFalse(historico.get(1).estaVigente());
    }

    @Test
    @DisplayName("7.4: el histórico va de la más reciente a la más antigua entre temporadas")
    void historicoOrdenadoDeLaMasReciente() {
        Apartamento apartamento = apartamentoNuevo();
        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(280_000)), ENERO);
        apartamento.definirTarifa(new Tarifa("T-ALTA", Dinero.de(560_000)), MARZO);
        apartamento.definirTarifa(new Tarifa("T-ALTA", Dinero.de(580_000)), MARZO);

        List<RegistroTarifa> historico = apartamento.getHistoricoTarifas();

        assertEquals(List.of(Dinero.de(580_000), Dinero.de(560_000), Dinero.de(280_000)),
                historico.stream().map(RegistroTarifa::valorPorOcupante).toList());
    }

    @Test
    @DisplayName("7.4: una tarifa no puede regir antes que la vigente")
    void rechazarTarifaConFechaAnteriorALaVigente() {
        Apartamento apartamento = apartamentoNuevo();
        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(280_000)), MARZO);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(300_000)), ENERO));

        assertTrue(error.getMessage().startsWith("7.4"));
        assertEquals(Dinero.de(280_000), apartamento.tarifaPara(BASE).valorPorOcupante());
    }

    @Test
    @DisplayName("7.4: una tarifa cargada sin fecha se conserva al reemplazarla, sin fecha de inicio conocida")
    void reemplazarTarifaCargadaSinFecha() {
        Apartamento apartamento = apartamentoActivo();               // T-BASE cargada sin fecha
        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(300_000)), MARZO);

        RegistroTarifa anterior = apartamento.getHistoricoTarifas().get(1);
        assertEquals(Dinero.de(280_000), anterior.valorPorOcupante());
        assertNull(anterior.vigenteDesde());
        assertEquals(MARZO, anterior.vigenteHasta());
    }

    @Test
    @DisplayName("7.4: si una temporada no existe o se repite no se aplica ninguna tarifa")
    void definirTarifasEsTodoONada() {
        Apartamento apartamento = apartamentoNuevo();

        ReglaDominioException inexistente = assertThrows(ReglaDominioException.class,
                () -> apartamento.definirTarifas(List.of(new Tarifa("T-BASE", Dinero.de(280_000)),
                        new Tarifa("T-MEDIA", Dinero.de(400_000))), List.of(BASE, ALTA), ENERO));
        ReglaDominioException repetida = assertThrows(ReglaDominioException.class,
                () -> apartamento.definirTarifas(List.of(new Tarifa("T-BASE", Dinero.de(280_000)),
                        new Tarifa("T-BASE", Dinero.de(290_000))), List.of(BASE, ALTA), ENERO));

        assertTrue(inexistente.getMessage().startsWith("7.4"));
        assertTrue(repetida.getMessage().startsWith("7.4"));
        assertTrue(apartamento.getTarifas().isEmpty());
        assertTrue(apartamento.getHistoricoTarifas().isEmpty());
    }

    @Test
    @DisplayName("CU-25 (7.4): un apartamento nuevo nace inactivo y no se activa sin tarifa en todas las temporadas")
    void activarSinTarifasCompletas() {
        Apartamento apartamento = apartamentoNuevo();
        assertFalse(apartamento.isActivo());

        apartamento.definirTarifas(List.of(new Tarifa("T-BASE", Dinero.de(280_000))), List.of(BASE, ALTA), ENERO);
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> apartamento.activar(List.of(BASE, ALTA)));

        assertTrue(error.getMessage().startsWith("7.4"));
        assertFalse(apartamento.isActivo());

        apartamento.definirTarifas(List.of(new Tarifa("T-ALTA", Dinero.de(560_000))), List.of(BASE, ALTA), ENERO);
        apartamento.activar(List.of(BASE, ALTA));
        assertTrue(apartamento.estaVigenteParaVenta());
    }

    private static Apartamento apartamentoNuevo() {
        return Apartamento.crear(new IdentificacionApartamento("APT-401"), "Coral Nuevo", "Vista a la bahía", 1,
                new Capacidad(2), new Dotacion(List.of("Balcón")), List.of(new Imagen("https://coral.co/apt-401.jpg", true)));
    }

    private static Apartamento apartamentoActivo() {
        Apartamento apartamento = Apartamento.crear(new IdentificacionApartamento("APT-202"), "Bahía Cristal",
                "Grupos familiares", 2, new Capacidad(4), new Dotacion(List.of("Cocina integral")),
                List.of(new Imagen("https://coral.co/apt-202.jpg", true)));
        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(280_000)));
        apartamento.activar(List.of(Temporada.crearBase("T-BASE", "Base")));
        return apartamento;
    }
}
