package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.RegistroTarifa;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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
 * Apartamento.definirTarifas() y Apartamento.activar();
 * 7.6 (transiciones de estado operativo), implementada en los métodos de negocio recibirGrupo, liberarTrasSalida,
 * iniciarPreparacion, terminarPreparacion, ponerFueraDeServicio y restablecerServicio.
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

    // ==================== 7.6: transiciones de estado operativo ====================

    @Test
    @DisplayName("7.6: ciclo permitido PREPARADO → OCUPADO → PENDIENTE_PREPARACION → EN_PREPARACION → PREPARADO")
    void cicloPermitidoDeEstadoOperativo() {
        Apartamento apartamento = apartamentoNuevo();
        assertEquals(EstadoOperativo.PREPARADO, apartamento.getEstadoOperativo());

        apartamento.recibirGrupo();
        assertEquals(EstadoOperativo.OCUPADO, apartamento.getEstadoOperativo());
        apartamento.liberarTrasSalida();
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, apartamento.getEstadoOperativo());
        apartamento.iniciarPreparacion();
        assertEquals(EstadoOperativo.EN_PREPARACION, apartamento.getEstadoOperativo());
        apartamento.terminarPreparacion();
        assertEquals(EstadoOperativo.PREPARADO, apartamento.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6: desde PREPARADO, PENDIENTE_PREPARACION o EN_PREPARACION se pone FUERA_DE_SERVICIO y se restablece")
    void ponerFueraDeServicioYRestablecerDesdeEstadosNoOcupados() {
        for (EstadoOperativo origen : List.of(EstadoOperativo.PREPARADO, EstadoOperativo.PENDIENTE_PREPARACION,
                EstadoOperativo.EN_PREPARACION)) {
            Apartamento apartamento = apartamentoEn(origen);

            apartamento.ponerFueraDeServicio();
            assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, apartamento.getEstadoOperativo(), "desde " + origen);
            apartamento.restablecerServicio();
            assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, apartamento.getEstadoOperativo(), "desde " + origen);
        }
    }

    @Test
    @DisplayName("7.6: PREPARADO → EN_PREPARACION no está permitido y el estado no cambia")
    void rechazarIniciarPreparacionSiYaEstaPreparado() {
        Apartamento apartamento = apartamentoNuevo();

        ReglaDominioException error = assertThrows(ReglaDominioException.class, apartamento::iniciarPreparacion);

        assertTrue(error.getMessage().startsWith("7.6"));
        assertEquals(EstadoOperativo.PREPARADO, apartamento.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6: OCUPADO → FUERA_DE_SERVICIO no está permitido con huéspedes adentro")
    void rechazarFueraDeServicioSiEstaOcupado() {
        Apartamento apartamento = apartamentoEn(EstadoOperativo.OCUPADO);

        ReglaDominioException error = assertThrows(ReglaDominioException.class, apartamento::ponerFueraDeServicio);

        assertTrue(error.getMessage().startsWith("7.6"));
        assertEquals(EstadoOperativo.OCUPADO, apartamento.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6: FUERA_DE_SERVICIO no salta a PREPARADO ni a EN_PREPARACION, ni se vuelve a poner fuera de servicio")
    void rechazarTransicionesDesdeFueraDeServicio() {
        Apartamento apartamento = apartamentoEn(EstadoOperativo.FUERA_DE_SERVICIO);

        assertThrows(ReglaDominioException.class, apartamento::terminarPreparacion);
        assertThrows(ReglaDominioException.class, apartamento::iniciarPreparacion);
        assertThrows(ReglaDominioException.class, apartamento::ponerFueraDeServicio);
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, apartamento.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6: no se libera tras salida ni se restablece el servicio de un apartamento PREPARADO")
    void rechazarLiberarORestablecerSiEstaPreparado() {
        Apartamento apartamento = apartamentoNuevo();

        assertThrows(ReglaDominioException.class, apartamento::liberarTrasSalida);
        assertThrows(ReglaDominioException.class, apartamento::restablecerServicio);
        assertThrows(ReglaDominioException.class, apartamento::terminarPreparacion);
        assertEquals(EstadoOperativo.PREPARADO, apartamento.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6 y RN-11: un apartamento PENDIENTE_PREPARACION no recibe grupo ni termina sin iniciar la preparación")
    void rechazarTransicionesDesdePendientePreparacion() {
        Apartamento apartamento = apartamentoEn(EstadoOperativo.PENDIENTE_PREPARACION);

        ReglaDominioException sinPreparar = assertThrows(ReglaDominioException.class, apartamento::recibirGrupo);
        assertTrue(sinPreparar.getMessage().startsWith("RN-11"));
        assertThrows(ReglaDominioException.class, apartamento::terminarPreparacion);
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, apartamento.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6: liberarTrasSalida desde FUERA_DE_SERVICIO se rechaza; solo se libera un apartamento OCUPADO")
    void rechazarLiberarTrasSalidaDesdeFueraDeServicio() {
        Apartamento apartamento = apartamentoEn(EstadoOperativo.FUERA_DE_SERVICIO);

        ReglaDominioException error = assertThrows(ReglaDominioException.class, apartamento::liberarTrasSalida);

        assertTrue(error.getMessage().startsWith("7.6"));
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, apartamento.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6: restablecerServicio desde OCUPADO se rechaza; solo se restablece un apartamento FUERA_DE_SERVICIO")
    void rechazarRestablecerServicioDesdeOcupado() {
        Apartamento apartamento = apartamentoEn(EstadoOperativo.OCUPADO);

        ReglaDominioException error = assertThrows(ReglaDominioException.class, apartamento::restablecerServicio);

        assertTrue(error.getMessage().startsWith("7.6"));
        assertEquals(EstadoOperativo.OCUPADO, apartamento.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6: cada método de negocio funciona desde su estado de origen y llega a su destino")
    void cadaMetodoFuncionaDesdeSuEstadoDeOrigen() {
        Apartamento preparado = apartamentoEn(EstadoOperativo.PREPARADO);
        preparado.recibirGrupo();
        assertEquals(EstadoOperativo.OCUPADO, preparado.getEstadoOperativo());

        Apartamento ocupado = apartamentoEn(EstadoOperativo.OCUPADO);
        ocupado.liberarTrasSalida();
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, ocupado.getEstadoOperativo());

        Apartamento pendiente = apartamentoEn(EstadoOperativo.PENDIENTE_PREPARACION);
        pendiente.iniciarPreparacion();
        assertEquals(EstadoOperativo.EN_PREPARACION, pendiente.getEstadoOperativo());

        Apartamento enPreparacion = apartamentoEn(EstadoOperativo.EN_PREPARACION);
        enPreparacion.terminarPreparacion();
        assertEquals(EstadoOperativo.PREPARADO, enPreparacion.getEstadoOperativo());

        Apartamento fueraDeServicio = apartamentoEn(EstadoOperativo.FUERA_DE_SERVICIO);
        fueraDeServicio.restablecerServicio();
        assertEquals(EstadoOperativo.PENDIENTE_PREPARACION, fueraDeServicio.getEstadoOperativo());
    }

    @Test
    @DisplayName("7.6: fuera de su estado de origen cada método lanza ReglaDominioException y el estado no cambia")
    void cadaMetodoRechazaLosDemasEstadosDeOrigen() {
        Map<String, Consumer<Apartamento>> metodos = new LinkedHashMap<>();
        Map<String, Set<EstadoOperativo>> origenes = new LinkedHashMap<>();
        metodos.put("recibirGrupo", Apartamento::recibirGrupo);
        origenes.put("recibirGrupo", EnumSet.of(EstadoOperativo.PREPARADO));
        metodos.put("liberarTrasSalida", Apartamento::liberarTrasSalida);
        origenes.put("liberarTrasSalida", EnumSet.of(EstadoOperativo.OCUPADO));
        metodos.put("iniciarPreparacion", Apartamento::iniciarPreparacion);
        origenes.put("iniciarPreparacion", EnumSet.of(EstadoOperativo.PENDIENTE_PREPARACION));
        metodos.put("terminarPreparacion", Apartamento::terminarPreparacion);
        origenes.put("terminarPreparacion", EnumSet.of(EstadoOperativo.EN_PREPARACION));
        metodos.put("ponerFueraDeServicio", Apartamento::ponerFueraDeServicio);
        origenes.put("ponerFueraDeServicio", EnumSet.complementOf(
                EnumSet.of(EstadoOperativo.OCUPADO, EstadoOperativo.FUERA_DE_SERVICIO)));
        metodos.put("restablecerServicio", Apartamento::restablecerServicio);
        origenes.put("restablecerServicio", EnumSet.of(EstadoOperativo.FUERA_DE_SERVICIO));

        metodos.forEach((nombre, metodo) -> {
            for (EstadoOperativo estado : EstadoOperativo.values()) {
                Apartamento apartamento = apartamentoEn(estado);
                if (origenes.get(nombre).contains(estado)) {
                    assertDoesNotThrow(() -> metodo.accept(apartamento), nombre + " desde " + estado);
                } else {
                    ReglaDominioException error = assertThrows(ReglaDominioException.class,
                            () -> metodo.accept(apartamento), nombre + " desde " + estado);
                    String codigoEsperado = nombre.equals("recibirGrupo") ? "RN-11" : "7.6";
                    assertTrue(error.getMessage().startsWith(codigoEsperado), nombre + ": " + error.getMessage());
                    assertEquals(estado, apartamento.getEstadoOperativo(), nombre + " no debía cambiar " + estado);
                }
            }
        });
    }

    /** Lleva un apartamento nuevo (PREPARADO) al estado pedido usando solo los métodos de negocio. */
    private static Apartamento apartamentoEn(EstadoOperativo estado) {
        Apartamento apartamento = apartamentoNuevo();
        switch (estado) {
            case PREPARADO -> { }
            case OCUPADO -> apartamento.recibirGrupo();
            case PENDIENTE_PREPARACION -> {
                apartamento.recibirGrupo();
                apartamento.liberarTrasSalida();
            }
            case EN_PREPARACION -> {
                apartamento.recibirGrupo();
                apartamento.liberarTrasSalida();
                apartamento.iniciarPreparacion();
            }
            case FUERA_DE_SERVICIO -> apartamento.ponerFueraDeServicio();
        }
        assertEquals(estado, apartamento.getEstadoOperativo());
        return apartamento;
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
