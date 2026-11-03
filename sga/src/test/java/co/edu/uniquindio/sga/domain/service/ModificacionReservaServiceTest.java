package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.service.ModificacionReservaService.DatosModificacion;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CargoAseoMascota;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.Mascotas;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-14, implementada en ModificacionReservaService.modificar y Reserva.modificar. */
class ModificacionReservaServiceTest {

    private static final LocalTime HORA_LLEGADA = LocalTime.of(16, 0);

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final CotizadorEstanciaService cotizador =
            new CotizadorEstanciaService(escenario.apartamentos, escenario.alojamientos);
    private final DisponibilidadApartamentoService disponibilidad = new DisponibilidadApartamentoService(
            escenario.reservas, escenario.apartamentos, escenario.bloqueos, escenario.alojamientos, cotizador);
    private final ModificacionReservaService modificacion = new ModificacionReservaService(escenario.reservas,
            escenario.folios, escenario.apartamentos, escenario.alojamientos, disponibilidad, cotizador);

    @Test
    @DisplayName("RN-14: la modificación revalida las condiciones y rechaza un rango no disponible")
    void rechazarModificacionAUnRangoNoDisponible() {
        // Martes 20 a jueves 22 y lunes 26 a miércoles 28 de octubre, en temporada base.
        Reserva otra = escenario.reservaPendiente(1, new Estancia(LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 10, 28)), HORA_LLEGADA);
        Reserva reserva = conFolio(escenario.reservaPendiente(2, new Estancia(LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 22)), HORA_LLEGADA));
        Estancia ocupada = otra.getEstancia();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> modificacion.modificar(reserva.getCodigo(), new DatosModificacion(null, ocupada, null),
                        EscenarioCoral.AHORA));

        assertTrue(error.getMessage().startsWith("RN-01"));
        assertEquals(new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22)), reserva.getEstancia());
        assertEquals(Dinero.de(620_000), reserva.valorTotal());
        assertTrue(ajustesDe(reserva).isEmpty());
    }

    @Test
    @DisplayName("RN-14: con un rango válido se recalcula el valor y se registra el ajuste en el folio")
    void recalcularValorYRegistrarAjuste() {
        Reserva reserva = conFolio(escenario.reservaPendiente(1, new Estancia(LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 22)), HORA_LLEGADA));
        // Una noche más en temporada base: de $620.000 a $930.000.
        Estancia nueva = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 23));

        Dinero diferencia = modificacion.modificar(reserva.getCodigo(), new DatosModificacion(null, nueva, null),
                EscenarioCoral.AHORA);

        assertEquals(Dinero.de(310_000), diferencia);
        assertEquals(nueva, reserva.getEstancia());
        assertEquals(Dinero.de(930_000), reserva.valorTotal());
        List<Cargo> ajustes = ajustesDe(reserva);
        assertEquals(1, ajustes.size());
        assertEquals(Dinero.de(310_000), ajustes.get(0).valor());
    }

    // ==================== RN-14, RN-16 y L-16: cargo de aseo de mascotas al modificar ====================

    /** Martes 20 a jueves 22 de octubre de 2026, temporada base. */
    private static final Estancia ESTANCIA_BASE = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));
    private static final Dinero CARGO_ASEO = Dinero.de(110_000);

    @Test
    @DisplayName("RN-14 y RN-16: quitar las mascotas revierte el cargo de aseo y el saldo baja en $110.000")
    void quitarMascotasRevierteElCargoDeAseo() {
        aceptarMascotas();
        Reserva reserva = conFolio(escenario.reservaPendiente(1, ESTANCIA_BASE, HORA_LLEGADA));
        modificar(reserva, new Mascotas(1, 8));
        Dinero saldoConMascotas = folioDe(reserva).saldo();

        Dinero diferencia = modificar(reserva, Mascotas.ninguna());

        assertEquals(Dinero.cero(), diferencia);
        assertFalse(reserva.getMascotas().traeMascotas());
        assertEquals(saldoConMascotas.restar(CARGO_ASEO), folioDe(reserva).saldo());
        List<Cargo> reversos = cargosDeAseo(reserva, CargoAseoMascota::esReversoDeCargoDeAseo);
        assertEquals(1, reversos.size());
        assertEquals(CARGO_ASEO.negar(), reversos.get(0).valor());
        assertTrue(reversos.get(0).concepto().contains("Modificación de la reserva: el grupo ya no trae mascotas"));
    }

    @Test
    @DisplayName("L-16: si después de quitarlas vuelve a traer mascotas, el cargo de aseo se cobra otra vez")
    void volverAAgregarMascotasCobraOtraVezElCargo() {
        aceptarMascotas();
        Reserva reserva = conFolio(escenario.reservaPendiente(1, ESTANCIA_BASE, HORA_LLEGADA));
        Dinero saldoSinMascotas = folioDe(reserva).saldo();
        modificar(reserva, new Mascotas(1, 8));
        modificar(reserva, Mascotas.ninguna());
        assertEquals(saldoSinMascotas, folioDe(reserva).saldo());

        modificar(reserva, new Mascotas(2, 9));

        assertEquals(saldoSinMascotas.sumar(CARGO_ASEO), folioDe(reserva).saldo());
        assertEquals(2, cargosDeAseo(reserva, CargoAseoMascota::esCargoDeAseo).size());
        assertEquals(1, cargosDeAseo(reserva, CargoAseoMascota::esReversoDeCargoDeAseo).size());
        // Y si se quitan de nuevo, se revierte el segundo cargo
        modificar(reserva, Mascotas.ninguna());
        assertEquals(saldoSinMascotas, folioDe(reserva).saldo());
        assertEquals(2, cargosDeAseo(reserva, CargoAseoMascota::esReversoDeCargoDeAseo).size());
    }

    @Test
    @DisplayName("L-16: cambiar solo la cantidad o el peso de las mascotas no cobra ni revierte el cargo de aseo")
    void cambiarSoloLaCantidadDeMascotasNoCobraNiRevierte() {
        aceptarMascotas();
        Reserva reserva = conFolio(escenario.reservaPendiente(1, ESTANCIA_BASE, HORA_LLEGADA));
        modificar(reserva, new Mascotas(1, 8));
        Dinero saldoConMascotas = folioDe(reserva).saldo();
        int cargosAntes = folioDe(reserva).getCargos().size();

        modificar(reserva, new Mascotas(2, 10));

        assertEquals(new Mascotas(2, 10), reserva.getMascotas());
        assertEquals(saldoConMascotas, folioDe(reserva).saldo());
        assertEquals(cargosAntes, folioDe(reserva).getCargos().size());
        assertEquals(1, cargosDeAseo(reserva, CargoAseoMascota::esCargoDeAseo).size());
        assertTrue(cargosDeAseo(reserva, CargoAseoMascota::esReversoDeCargoDeAseo).isEmpty());
    }

    @Test
    @DisplayName("RN-16: si la reserva traía mascotas pero el folio no tiene el cargo de aseo, no se inventa un movimiento")
    void rechazarRevertirUnCargoDeAseoQueNoEstaEnElFolio() {
        aceptarMascotas();
        Reserva reserva = conFolio(reservaConMascotasSinCargo(new Mascotas(1, 8)));
        int cargosAntes = folioDe(reserva).getCargos().size();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> modificar(reserva, Mascotas.ninguna()));

        assertTrue(error.getMessage().startsWith("RN-16"));
        assertEquals(new Mascotas(1, 8), reserva.getMascotas());
        assertEquals(cargosAntes, folioDe(reserva).getCargos().size());
    }

    /** Anexo A (L-16): máximo 2 mascotas de hasta 10 kg y cargo único de aseo de $110.000. */
    private void aceptarMascotas() {
        escenario.alojamiento.definirPoliticaMascotas(new CargoAseoMascota(true, CARGO_ASEO, 2, 10));
        escenario.alojamientos.guardar(escenario.alojamiento);
    }

    private Dinero modificar(Reserva reserva, Mascotas mascotas) {
        return modificacion.modificar(reserva.getCodigo(), new DatosModificacion(null, null, null, mascotas),
                EscenarioCoral.AHORA);
    }

    private Folio folioDe(Reserva reserva) {
        return escenario.folios.buscarPorReserva(reserva.getCodigo()).orElseThrow();
    }

    private List<Cargo> cargosDeAseo(Reserva reserva, Predicate<Cargo> filtro) {
        return folioDe(reserva).getCargos().stream().filter(filtro).toList();
    }

    /** Reserva que ya trae mascotas, con un folio al que nunca se le asentó el cargo de aseo. */
    private Reserva reservaConMascotasSinCargo(Mascotas mascotas) {
        Reserva reserva = Reserva.crear(CodigoReserva.generar(2026, 9), escenario.apartamento, ESTANCIA_BASE,
                escenario.titular, List.of(escenario.ana), CanalOrigen.PORTAL, null, HORA_LLEGADA,
                cotizador.cotizar(escenario.apartamento, ESTANCIA_BASE, List.of(escenario.ana), null), mascotas,
                escenario.politica, escenario.alojamiento.getUmbralEdadFacturable(), EscenarioCoral.AHORA);
        escenario.reservas.guardar(reserva);
        return reserva;
    }

    /** Folio abierto como en CU-03: cargo de alojamiento y depósito de garantía (RP-02). */
    private Reserva conFolio(Reserva reserva) {
        LocalDate apertura = EscenarioCoral.AHORA.toLocalDate();
        escenario.folios.guardar(Folio.abrir("FOL-" + reserva.getCodigo().valor(), reserva.getCodigo(),
                new Cargo(TipoCargo.ALOJAMIENTO, "Alojamiento", reserva.valorTotal(), apertura),
                DepositoGarantia.de("Depósito de garantía", Dinero.de(250_000)), apertura));
        return reserva;
    }

    /**
     * Ajustes por modificación (RN-14). El depósito de garantía también es AJUSTE_MODIFICACION (RP-02, 7.7),
     * así que se distinguen por el concepto que asienta ModificacionReservaService.
     */
    private List<Cargo> ajustesDe(Reserva reserva) {
        return escenario.folios.buscarPorReserva(reserva.getCodigo()).orElseThrow().getCargos().stream()
                .filter(cargo -> cargo.tipo() == TipoCargo.AJUSTE_MODIFICACION)
                .filter(cargo -> cargo.concepto().startsWith("Ajuste por modificación"))
                .toList();
    }
}
