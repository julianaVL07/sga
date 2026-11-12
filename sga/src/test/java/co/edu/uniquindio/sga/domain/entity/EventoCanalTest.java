package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 7.8: entradas de la bitácora de los canales, incluidos los intentos que no se autenticaron (10.3.1). */
class EventoCanalTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 12, 9, 30);

    @Test
    @DisplayName("RN-19: una creación procesada debe traer el identificador externo")
    void creacionSinIdentificador() {
        assertThrows(ReglaDominioException.class, () -> EventoCanal.auditar("EVT-1", TipoEventoCanal.CREACION_RESERVA,
                CanalOrigen.EXTERNO, "booking", " ", ResultadoEventoCanal.ACEPTADO, AHORA));
    }

    @Test
    @DisplayName("10.3.1: un intento rechazado por la credencial queda en la bitácora aunque no se leyó el mensaje")
    void rechazoDeAccesoSinIdentificador() {
        EventoCanal evento = EventoCanal.auditar("EVT-1", TipoEventoCanal.CREACION_RESERVA, CanalOrigen.EXTERNO,
                "booking", null, ResultadoEventoCanal.RECHAZADO_CREDENCIAL, AHORA);

        assertNull(evento.getIdentificadorExterno());
        assertEquals("booking", evento.getNombreCanal());
        assertTrue(evento.getResultado().esRechazoDeAcceso());
        assertTrue(ResultadoEventoCanal.RECHAZADO_CANAL_INACTIVO.esRechazoDeAcceso());
        assertFalse(ResultadoEventoCanal.RECHAZADO_CONFLICTO.esRechazoDeAcceso());
    }

    @Test
    @DisplayName("7.8: todo evento registra el nombre del canal")
    void eventoSinCanal() {
        assertThrows(ReglaDominioException.class, () -> EventoCanal.auditar("EVT-1",
                TipoEventoCanal.CONSULTA_DISPONIBILIDAD, CanalOrigen.EXTERNO, " ", null, ResultadoEventoCanal.ACEPTADO,
                AHORA));
    }

    @Test
    @DisplayName("RN-19: un rechazo de acceso nunca cuenta como mensaje ya aceptado")
    void rechazoNoEsDuplicado() {
        EventoCanal rechazo = EventoCanal.auditar("EVT-1", TipoEventoCanal.CANCELACION_RESERVA, CanalOrigen.EXTERNO,
                "booking", "BK-1", ResultadoEventoCanal.RECHAZADO_CREDENCIAL, AHORA);

        assertFalse(rechazo.esDuplicado("booking", "BK-1", TipoEventoCanal.CANCELACION_RESERVA));
    }

    @Test
    @DisplayName("RN-19: el duplicado es por nombre del canal: el mismo identificador desde otro canal no lo es")
    void duplicadoPorNombreDeCanal() {
        EventoCanal aceptado = EventoCanal.auditar("EVT-1", TipoEventoCanal.CREACION_RESERVA, CanalOrigen.EXTERNO,
                "booking", "BK-1", ResultadoEventoCanal.ACEPTADO, AHORA);

        assertTrue(aceptado.esDuplicado("booking", "BK-1", TipoEventoCanal.CREACION_RESERVA));
        assertFalse(aceptado.esDuplicado("airbnb", "BK-1", TipoEventoCanal.CREACION_RESERVA));
        assertFalse(aceptado.esDuplicado("booking", "BK-1", TipoEventoCanal.CANCELACION_RESERVA));
    }

    @Test
    @DisplayName("7.8: el filtro de la bitácora es por canal (sin distinguir mayúsculas) y por fechas incluidas")
    void filtroDeLaBitacora() {
        EventoCanal evento = EventoCanal.auditar("EVT-1", TipoEventoCanal.CONSULTA_DISPONIBILIDAD,
                CanalOrigen.EXTERNO, "booking", null, ResultadoEventoCanal.ACEPTADO, AHORA);
        LocalDate hoy = AHORA.toLocalDate();

        assertTrue(evento.cumpleFiltro(null, null, null));
        assertTrue(evento.cumpleFiltro("Booking", hoy, hoy));
        assertFalse(evento.cumpleFiltro("airbnb", null, null));
        assertFalse(evento.cumpleFiltro(null, hoy.plusDays(1), null));
        assertFalse(evento.cumpleFiltro(null, null, hoy.minusDays(1)));
    }
}
