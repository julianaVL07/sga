package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CargoAseoMascota;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.Mascotas;
import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ServicioAdicionalRepositoryInMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RP-03 (recargo por llegada nocturna) y L-16 (mascotas y cargo de aseo), implementadas
 * en CreacionReservaService.registrarNuevaReserva.
 */
class CreacionReservaServiceTest {

    private static final String ID_RECEPCION_NOCTURNA = "SRV-RECEPCION-NOCTURNA";

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final CotizadorEstanciaService cotizador =
            new CotizadorEstanciaService(escenario.apartamentos, escenario.alojamientos);
    private final DisponibilidadApartamentoService disponibilidad = new DisponibilidadApartamentoService(
            escenario.reservas, escenario.apartamentos, escenario.bloqueos, escenario.alojamientos, cotizador);
    private final CreacionReservaService creacion = new CreacionReservaService(escenario.reservas, escenario.folios,
            escenario.apartamentos, escenario.alojamientos, new ServicioAdicionalRepositoryInMemory(), disponibilidad,
            cotizador, DepositoGarantia.de("Depósito de garantía", Dinero.de(250_000)), ID_RECEPCION_NOCTURNA);

    // Martes 20 a jueves 22 de octubre de 2026, en temporada base.
    private final Estancia estancia = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));

    @BeforeEach
    void configurarServicioNocturnoYPoliticaDeMascotas() {
        escenario.alojamiento.agregarServicioAdicional(ServicioAdicional.crearConCargo(ID_RECEPCION_NOCTURNA,
                "Recepción nocturna", "Atención de llegadas desde las 20:00", Dinero.de(70_000)));
        // Anexo A: máximo 2 mascotas de hasta 10 kg, cargo único de $110.000.
        escenario.alojamiento.definirPoliticaMascotas(new CargoAseoMascota(true, Dinero.de(110_000), 2, 10));
        escenario.alojamientos.guardar(escenario.alojamiento);
    }

    // ---------------------------------------------------------------- RP-03

    @Test
    @DisplayName("RP-03: una llegada a las 20:00 o después genera el cargo de recepción nocturna")
    void llegadaNocturnaGeneraRecargo() {
        Reserva aLasOcho = crear(LocalTime.of(20, 0), Mascotas.ninguna());
        Reserva masTarde = crear(LocalTime.of(23, 30), Mascotas.ninguna(),
                new Estancia(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 28)));

        for (Reserva reserva : List.of(aLasOcho, masTarde)) {
            List<Cargo> servicios = serviciosAdicionales(reserva);
            assertEquals(1, servicios.size());
            assertEquals("Recepción nocturna", servicios.get(0).concepto());
            assertEquals(Dinero.de(70_000), servicios.get(0).valor());
        }
    }

    @Test
    @DisplayName("RP-03: una llegada antes de las 20:00 no genera el cargo de recepción nocturna")
    void llegadaAntesDeLasOchoNoGeneraRecargo() {
        Reserva reserva = crear(LocalTime.of(19, 59), Mascotas.ninguna());

        assertTrue(serviciosAdicionales(reserva).isEmpty());
    }

    // ---------------------------------------------------------------- L-16

    @Test
    @DisplayName("L-16: más de 2 mascotas se rechaza y no se crea la reserva")
    void rechazarMasDeDosMascotas() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> crear(LocalTime.of(16, 0), new Mascotas(3, 5)));

        assertTrue(error.getMessage().startsWith("L-16"));
        assertTrue(escenario.reservas.listarTodas().isEmpty());
    }

    @Test
    @DisplayName("L-16: una mascota de más de 10 kg se rechaza y no se crea la reserva")
    void rechazarMascotaDeMasDeDiezKilos() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> crear(LocalTime.of(16, 0), new Mascotas(1, 11)));

        assertTrue(error.getMessage().startsWith("L-16"));
        assertTrue(escenario.reservas.listarTodas().isEmpty());
    }

    @Test
    @DisplayName("L-16: con mascotas válidas se crea la reserva y se asienta el cargo de aseo en el folio")
    void mascotasValidasGeneranCargoDeAseo() {
        Reserva reserva = crear(LocalTime.of(16, 0), new Mascotas(2, 10));

        assertEquals(new Mascotas(2, 10), reserva.getMascotas());
        List<Cargo> servicios = serviciosAdicionales(reserva);
        assertEquals(1, servicios.size());
        assertEquals("Cargo por aseo de mascota", servicios.get(0).concepto());
        assertEquals(Dinero.de(110_000), servicios.get(0).valor());
    }

    @Test
    @DisplayName("L-16: sin mascotas no se asienta el cargo de aseo")
    void sinMascotasNoHayCargoDeAseo() {
        Reserva reserva = crear(LocalTime.of(16, 0), Mascotas.ninguna());

        assertEquals(Mascotas.ninguna(), reserva.getMascotas());
        assertTrue(serviciosAdicionales(reserva).isEmpty());
    }

    // ---------------------------------------------------------------- Apoyo

    private Reserva crear(LocalTime horaLlegada, Mascotas mascotas) {
        return crear(horaLlegada, mascotas, estancia);
    }

    private Reserva crear(LocalTime horaLlegada, Mascotas mascotas, Estancia estanciaReserva) {
        return creacion.registrarNuevaReserva(EscenarioCoral.APT_101, estanciaReserva, escenario.titular,
                List.of(escenario.ana), horaLlegada, null, escenario.politica, CanalOrigen.PORTAL, null, mascotas,
                EscenarioCoral.AHORA);
    }

    private List<Cargo> serviciosAdicionales(Reserva reserva) {
        Folio folio = escenario.folios.buscarPorReserva(reserva.getCodigo()).orElseThrow();
        return folio.getCargos().stream().filter(cargo -> cargo.tipo() == TipoCargo.SERVICIO_ADICIONAL).toList();
    }
}
