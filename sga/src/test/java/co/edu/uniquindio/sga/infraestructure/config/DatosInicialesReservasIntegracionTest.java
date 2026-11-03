package co.edu.uniquindio.sga.infraestructure.config;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Datos de prueba de reservas (12.3): al arrancar la aplicación completa, DatosInicialesReservasConfig deja
 * reservas en los 6 estados de la sección 8 y en los 3 canales de origen, cada una con su folio (F-08).
 */
@SpringBootTest
class DatosInicialesReservasIntegracionTest {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private FolioRepository folioRepository;

    @Autowired
    private ApartamentoRepository apartamentoRepository;

    @Test
    @DisplayName("12.3: hay al menos una reserva en cada uno de los 6 estados")
    void hayReservasEnTodosLosEstados() {
        for (EstadoReserva estado : EstadoReserva.values()) {
            FiltroReservas filtro = new FiltroReservas(estado, null, null, null, null, null);
            assertTrue(reservaRepository.listar(filtro, 0, 10).totalElementos() > 0,
                    "No hay reservas en estado " + estado);
        }
    }

    @Test
    @DisplayName("12.3: hay al menos una reserva por cada canal, y la externa trae su identificador (RN-19)")
    void hayReservasEnTodosLosCanales() {
        for (CanalOrigen canal : CanalOrigen.values()) {
            FiltroReservas filtro = new FiltroReservas(null, null, canal, null, null, null);
            assertTrue(reservaRepository.listar(filtro, 0, 10).totalElementos() > 0,
                    "No hay reservas del canal " + canal);
        }
        assertTrue(reservaRepository.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, "BK-DEMO-000123")
                .isPresent());
    }

    @Test
    @DisplayName("F-08: cada reserva tiene su folio")
    void cadaReservaTieneSuFolio() {
        for (Reserva reserva : reservaRepository.listarTodas()) {
            assertTrue(folioRepository.buscarPorReserva(reserva.getCodigo()).isPresent(),
                    "La reserva " + reserva.getCodigo().valor() + " no tiene folio");
        }
    }

    @Test
    @DisplayName("RN-11: la reserva EN_CURSO deja su apartamento OCUPADO")
    void laReservaEnCursoOcupaSuApartamento() {
        FiltroReservas filtro = new FiltroReservas(EstadoReserva.EN_CURSO, new IdentificacionApartamento("APT-201"),
                null, null, null, null);
        assertFalse(reservaRepository.listar(filtro, 0, 10).contenido().isEmpty());
        assertEquals(EstadoOperativo.OCUPADO, apartamentoRepository
                .buscarPorIdentificacion(new IdentificacionApartamento("APT-201")).orElseThrow().getEstadoOperativo());
    }
}
