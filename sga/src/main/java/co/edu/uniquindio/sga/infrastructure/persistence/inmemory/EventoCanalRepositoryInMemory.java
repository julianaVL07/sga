package co.edu.uniquindio.sga.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria (I-02) de la bitácora. Solo agrega: un id repetido se rechaza,
 * porque un evento auditado nunca se reemplaza.
 */
public class EventoCanalRepositoryInMemory implements EventoCanalRepository {

    private static final Comparator<EventoCanal> MAS_RECIENTE_PRIMERO =
            Comparator.comparing(EventoCanal::getFecha).reversed();

    private final Map<String, EventoCanal> almacen = new ConcurrentHashMap<>();

    @Override
    public EventoCanal guardar(EventoCanal evento) {
        if (evento == null) {
            throw new IllegalArgumentException("El evento a guardar es obligatorio");
        }
        if (almacen.putIfAbsent(evento.getId(), evento) != null) {
            throw new IllegalStateException("La bitácora ya tiene el evento " + evento.getId());
        }
        return evento;
    }

    @Override
    public List<EventoCanal> listarBitacora() {
        return almacen.values().stream().sorted(MAS_RECIENTE_PRIMERO).toList();
    }

    @Override
    public List<EventoCanal> listarPorCanalEIdentificador(CanalOrigen canal, String identificadorExterno) {
        return almacen.values().stream()
                .filter(e -> e.getCanalOrigen() == canal)
                .filter(e -> identificadorExterno != null && identificadorExterno.equals(e.getIdentificadorExterno()))
                .sorted(MAS_RECIENTE_PRIMERO)
                .toList();
    }

    @Override
    public boolean existeDuplicado(CanalOrigen canal, String identificadorExterno, TipoEventoCanal tipo) {
        return almacen.values().stream().anyMatch(e -> e.esDuplicado(canal, identificadorExterno, tipo));
    }
}
