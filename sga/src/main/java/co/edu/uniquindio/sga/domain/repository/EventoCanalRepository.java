package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;

import java.util.List;

/**
 * Puerto de la bitácora de eventos con los canales (7.8). Solo se agregan eventos: nunca se modifican ni borran.
 */
public interface EventoCanalRepository {

    EventoCanal guardar(EventoCanal evento);

    /** Bitácora completa, del evento más reciente al más antiguo. */
    List<EventoCanal> listarBitacora();

    List<EventoCanal> listarPorCanalEIdentificador(CanalOrigen canal, String identificadorExterno);

    /** RN-19: ¿ya se aceptó un mensaje con el mismo canal, identificador externo y tipo? */
    boolean existeDuplicado(CanalOrigen canal, String identificadorExterno, TipoEventoCanal tipo);
}
