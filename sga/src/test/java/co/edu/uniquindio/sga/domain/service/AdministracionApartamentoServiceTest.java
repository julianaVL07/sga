package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoActualizacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: 7.3 (CU-22, CU-23: advertencia de capacidad) y 7.4 (CU-24, CU-25), implementadas en
 * AdministracionApartamentoService y en el agregado Apartamento.
 */
class AdministracionApartamentoServiceTest {

    private static final LocalDate HOY = EscenarioCoral.AHORA.toLocalDate();

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final AdministracionApartamentoService administracion = new AdministracionApartamentoService(
            escenario.apartamentos, escenario.alojamientos, escenario.reservas);

    @Test
    @DisplayName("7.3: bajar la capacidad lista las reservas activas o futuras que la superan, sin modificarlas")
    void bajarCapacidadAdviertePorReservasAfectadas() {
        Reserva grupoDeTres = reserva(1, new Estancia(HOY.plusDays(20), HOY.plusDays(22)), 3);
        Reserva grupoDeUno = reserva(2, new Estancia(HOY.plusDays(30), HOY.plusDays(32)), 1);

        ResultadoActualizacionApartamento resultado = administracion.actualizar(EscenarioCoral.APT_101,
                "Apartamento 101", "Vista al mar", 1, new Dotacion(List.of("Aire acondicionado")), new Capacidad(2),
                HOY);

        assertTrue(resultado.tieneAdvertencia());
        assertEquals(List.of(grupoDeTres.getCodigo()), resultado.reservasSobreCapacidad());
        assertEquals(2, resultado.apartamento().getCapacidad().maximo());
        assertEquals(3, escenario.reservas.buscarPorCodigo(grupoDeTres.getCodigo()).orElseThrow().totalOcupantes());
        assertTrue(escenario.reservas.buscarPorCodigo(grupoDeUno.getCodigo()).orElseThrow().estaActiva());
    }

    @Test
    @DisplayName("7.3: si la nueva capacidad cubre todas las reservas no hay advertencia")
    void capacidadSuficienteSinAdvertencia() {
        reserva(1, new Estancia(HOY.plusDays(20), HOY.plusDays(22)), 2);

        ResultadoActualizacionApartamento resultado = administracion.actualizar(EscenarioCoral.APT_101,
                "Coral Luminoso", null, 1, new Dotacion(List.of("Aire acondicionado")), new Capacidad(2), HOY);

        assertFalse(resultado.tieneAdvertencia());
        assertEquals("Coral Luminoso", resultado.apartamento().getNombre());
    }

    @Test
    @DisplayName("CU-22: un apartamento nuevo nace inactivo; con identificación repetida responde L-04")
    void registrarApartamento() {
        Apartamento nuevo = administracion.registrar(nuevoApartamento("APT-401"),
                List.of(new Tarifa("T-BASE", Dinero.de(300_000))), HOY);

        assertFalse(nuevo.isActivo());
        assertTrue(escenario.alojamiento.contieneApartamento(nuevo.getIdentificacion()));
        assertEquals(1, nuevo.getHistoricoTarifas().size());

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> administracion.registrar(nuevoApartamento("APT-101"), List.of(), HOY));
        assertTrue(error.getMessage().startsWith("L-04"));
    }

    @Test
    @DisplayName("7.4: definir tarifas para una temporada que no existe responde 7.4 y no cambia nada")
    void tarifaDeTemporadaInexistente() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> administracion.definirTarifas(EscenarioCoral.APT_101, List.of(
                        new Tarifa("T-BASE", Dinero.de(400_000)), new Tarifa("T-NAVIDAD", Dinero.de(900_000))), HOY));

        assertTrue(error.getMessage().startsWith("7.4"));
        Apartamento apartamento = escenario.apartamentos.buscarPorIdentificacion(EscenarioCoral.APT_101).orElseThrow();
        assertTrue(apartamento.getHistoricoTarifas().isEmpty());
    }

    @Test
    @DisplayName("CU-25 (7.4): sin tarifa en todas las temporadas no se activa; al completarlas sí")
    void activarExigeTarifasCompletas() {
        administracion.registrar(nuevoApartamento("APT-402"), List.of(new Tarifa("T-BASE", Dinero.de(300_000))), HOY);
        IdentificacionApartamento apt402 = new IdentificacionApartamento("APT-402");

        ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> administracion.activar(apt402));
        assertTrue(error.getMessage().startsWith("7.4"));

        administracion.definirTarifas(apt402, List.of(new Tarifa("T-ALTA", Dinero.de(600_000))), HOY);
        assertTrue(administracion.activar(apt402).estaVigenteParaVenta());
    }

    private Apartamento nuevoApartamento(String id) {
        return Apartamento.crear(new IdentificacionApartamento(id), "Nuevo " + id, "Vista a la bahía", 1,
                new Capacidad(2), new Dotacion(List.of("Balcón")), List.of(new Imagen("https://coral.co/n.jpg", true)));
    }

    /** Reserva PENDIENTE de APT-101 con el titular y {@code ocupantes - 1} acompañantes adultos. */
    private Reserva reserva(int secuencia, Estancia estancia, int ocupantes) {
        List<Ocupante> grupo = new java.util.ArrayList<>(List.of(escenario.ana));
        for (int i = 1; i < ocupantes; i++) {
            grupo.add(new Ocupante("Acompañante " + i, new DocumentoIdentidad("CC", "10940000" + i + secuencia),
                    LocalDate.of(1985, 1, i)));
        }
        CotizadorEstanciaService cotizador = new CotizadorEstanciaService(escenario.apartamentos, escenario.alojamientos);
        Reserva reserva = Reserva.crear(CodigoReserva.generar(2026, secuencia), escenario.apartamento, estancia,
                escenario.titular, grupo, CanalOrigen.PORTAL, null, null,
                cotizador.cotizar(escenario.apartamento, estancia, grupo, null), escenario.politica,
                escenario.alojamiento.getUmbralEdadFacturable(), EscenarioCoral.AHORA);
        escenario.reservas.guardar(reserva);
        return reserva;
    }
}
