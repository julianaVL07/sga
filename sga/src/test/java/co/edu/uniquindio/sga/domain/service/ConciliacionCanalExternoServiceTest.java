package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.service.ConciliacionCanalExternoService.DatosReservaExterna;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ConflictoCanalRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.EventoCanalRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.PoliticaCancelacionRepositoryInMemory;
import co.edu.uniquindio.sga.infrastructure.persistence.inmemory.ServicioAdicionalRepositoryInMemory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-18 (una reserva externa sobre noches ocupadas se rechaza y queda como conflicto) y
 * RN-19 (canal + identificador externo + tipo es único: un mensaje repetido no crea otra reserva), implementadas en
 * ConciliacionCanalExternoService.procesarMensajeExterno. Todo mensaje queda en la bitácora (7.8).
 * Escenario: Coral con APT-101 activo; las estancias van de lunes a miércoles en temporada base (sin RP-01).
 */
class ConciliacionCanalExternoServiceTest {

    private static final Estancia OCUPADA = new Estancia(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22));
    private static final String BOOKING = "booking";
    private static final String AIRBNB = "airbnb";
    private static final Estancia LIBRE =new Estancia(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 28));

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final EventoCanalRepositoryInMemory eventos = new EventoCanalRepositoryInMemory();
    private final ConflictoCanalRepositoryInMemory conflictos = new ConflictoCanalRepositoryInMemory();
    private final ConciliacionCanalExternoService conciliacion;

    ConciliacionCanalExternoServiceTest() {
        DisponibilidadApartamentoService disponibilidad = new DisponibilidadApartamentoService(escenario.reservas,
                escenario.apartamentos, escenario.bloqueos, escenario.alojamientos);
        CreacionReservaService creacion = new CreacionReservaService(escenario.reservas, escenario.folios,
                escenario.apartamentos, escenario.alojamientos, new ServicioAdicionalRepositoryInMemory(),
                disponibilidad, new CotizadorEstanciaService(escenario.apartamentos, escenario.alojamientos),
                DepositoGarantia.de("Depósito de garantía", Dinero.de(250_000)), "SRV-RECEPCION-NOCTURNA");
        PoliticaCancelacionRepositoryInMemory politicas = new PoliticaCancelacionRepositoryInMemory();
        politicas.guardar(escenario.politica);
        conciliacion = new ConciliacionCanalExternoService(escenario.reservas, escenario.apartamentos, eventos,
                conflictos, disponibilidad, creacion, new CancelarReservaService(escenario.reservas, escenario.folios),
                new PoliticaCancelacionService(politicas));
    }

    // ---------------------------------------------------------------- RN-18

    @Test
    @DisplayName("RN-18: una reserva externa que choca con una activa se rechaza, queda un conflicto y la existente no "
            + "cambia")
    void reservaExternaSobreNochesOcupadas() {
        Reserva existente = escenario.reservaPendiente(1, OCUPADA, LocalTime.of(16, 0));
        Estancia solapada = new Estancia(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 23));

        ResultadoEventoCanal resultado = recibir("BK-100", solapada);

        assertEquals(ResultadoEventoCanal.RECHAZADO_CONFLICTO, resultado);
        List<ConflictoCanal> registrados = conflictos.listarTodos();
        assertEquals(1, registrados.size());
        assertEquals("BK-100", registrados.get(0).getIdentificadorExterno());
        assertTrue(registrados.get(0).estaPendiente());
        // La reserva existente sigue igual y no se creó otra
        assertEquals(1, escenario.reservas.listarTodas().size());
        Reserva despues = escenario.reservas.buscarPorCodigo(existente.getCodigo()).orElseThrow();
        assertEquals(EstadoReserva.PENDIENTE, despues.getEstado());
        assertEquals(OCUPADA, despues.getEstancia());
        assertTrue(escenario.reservas.buscarPorCanalEIdentificadorExterno(BOOKING, "BK-100").isEmpty());
        assertEquals(List.of(ResultadoEventoCanal.RECHAZADO_CONFLICTO), resultadosEnBitacora("BK-100"));
    }

    @Test
    @DisplayName("RN-18: si las noches están libres la reserva externa se acepta y no se registra conflicto")
    void reservaExternaSobreNochesLibres() {
        escenario.reservaPendiente(1, OCUPADA, LocalTime.of(16, 0));

        ResultadoEventoCanal resultado = recibir("BK-200", LIBRE);

        assertEquals(ResultadoEventoCanal.ACEPTADO, resultado);
        assertTrue(conflictos.listarTodos().isEmpty());
        Reserva creada = escenario.reservas.buscarPorCanalEIdentificadorExterno(BOOKING, "BK-200")
                .orElseThrow();
        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
        assertEquals(CanalOrigen.EXTERNO, creada.getCanalOrigen());
        assertTrue(escenario.folios.buscarPorReserva(creada.getCodigo()).isPresent());
    }

    // ---------------------------------------------------------------- RN-19

    @Test
    @DisplayName("RN-19: el mismo mensaje dos veces no crea dos reservas; el segundo queda como DUPLICADO_IGNORADO")
    void mensajeRepetidoSeIgnora() {
        ResultadoEventoCanal primero = recibir("BK-300", LIBRE);
        ResultadoEventoCanal segundo = recibir("BK-300", LIBRE);

        assertEquals(ResultadoEventoCanal.ACEPTADO, primero);
        assertEquals(ResultadoEventoCanal.DUPLICADO_IGNORADO, segundo);
        assertEquals(1, escenario.reservas.listarTodas().size());
        List<ResultadoEventoCanal> bitacora = resultadosEnBitacora("BK-300");
        assertEquals(2, bitacora.size());
        assertTrue(bitacora.contains(ResultadoEventoCanal.ACEPTADO));
        assertTrue(bitacora.contains(ResultadoEventoCanal.DUPLICADO_IGNORADO));
        assertTrue(conflictos.listarTodos().isEmpty());              // un duplicado no es un conflicto (RN-18)
    }

    @Test
    @DisplayName("RN-19: un mensaje nuevo, con otro identificador externo, se acepta y crea su propia reserva")
    void mensajeNuevoSeAcepta() {
        recibir("BK-300", LIBRE);
        Estancia otra = new Estancia(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4));

        ResultadoEventoCanal resultado = recibir("BK-301", otra);

        assertEquals(ResultadoEventoCanal.ACEPTADO, resultado);
        assertEquals(2, escenario.reservas.listarTodas().size());
        assertTrue(escenario.reservas.buscarPorCanalEIdentificadorExterno(BOOKING, "BK-301").isPresent());
        assertEquals(List.of(ResultadoEventoCanal.ACEPTADO), resultadosEnBitacora("BK-301"));
    }

    @Test
    @DisplayName("RN-19: el mismo identificador desde dos canales distintos crea dos reservas; desde el mismo canal no "
            + "duplica")
    void mismoIdentificadorEnDosCanales() {
        Estancia otra = new Estancia(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4));

        assertEquals(ResultadoEventoCanal.ACEPTADO, recibir(BOOKING, "BK-400", LIBRE));
        assertEquals(ResultadoEventoCanal.ACEPTADO, recibir(AIRBNB, "BK-400", otra));
        assertEquals(ResultadoEventoCanal.DUPLICADO_IGNORADO, recibir(BOOKING, "BK-400", LIBRE));

        assertEquals(2, escenario.reservas.listarTodas().size());
        Reserva deBooking = escenario.reservas.buscarPorCanalEIdentificadorExterno(BOOKING, "BK-400").orElseThrow();
        Reserva deAirbnb = escenario.reservas.buscarPorCanalEIdentificadorExterno(AIRBNB, "BK-400").orElseThrow();
        assertEquals(BOOKING, deBooking.getNombreCanalExterno());
        assertEquals(AIRBNB, deAirbnb.getNombreCanalExterno());
        assertEquals(LIBRE, deBooking.getEstancia());
        assertEquals(otra, deAirbnb.getEstancia());
    }

    @Test
    @DisplayName("RN-19: un canal solo cancela las reservas que él mismo creó, aunque otro use el mismo identificador")
    void cadaCanalCancelaLaSuya() {
        recibir(BOOKING, "BK-500", LIBRE);

        ResultadoEventoCanal resultado = conciliacion.procesarMensajeExterno(CanalOrigen.EXTERNO, AIRBNB, "BK-500",
                TipoEventoCanal.CANCELACION_RESERVA, null, EscenarioCoral.AHORA);

        assertEquals(ResultadoEventoCanal.RECHAZADO_INVALIDO, resultado);
        assertEquals(EstadoReserva.PENDIENTE,
                escenario.reservas.buscarPorCanalEIdentificadorExterno(BOOKING, "BK-500").orElseThrow().getEstado());
    }

    private ResultadoEventoCanal recibir(String identificadorExterno, Estancia estancia) {
        return recibir(BOOKING, identificadorExterno, estancia);
    }

    private ResultadoEventoCanal recibir(String canal, String identificadorExterno, Estancia estancia) {
        DatosReservaExterna datos = new DatosReservaExterna(EscenarioCoral.APT_101, estancia, escenario.titular,
                List.of(escenario.ana), LocalTime.of(16, 0));
        return conciliacion.procesarMensajeExterno(CanalOrigen.EXTERNO, canal, identificadorExterno,
                TipoEventoCanal.CREACION_RESERVA, datos, EscenarioCoral.AHORA);
    }

    /** 7.8: resultados que quedaron en la bitácora para ese identificador externo. */
    private List<ResultadoEventoCanal> resultadosEnBitacora(String identificadorExterno) {
        return eventos.listarPorCanalEIdentificador(CanalOrigen.EXTERNO, identificadorExterno).stream()
                .map(EventoCanal::getResultado)
                .toList();
    }
}
