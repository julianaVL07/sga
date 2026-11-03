package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.repository.EventoCanalRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.CanalPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador JPA del puerto EventoCanalRepository (7.8). La bitácora es de solo escritura: guardar inserta y
 * rechaza un id que ya existe, nunca actualiza una fila.
 */
@Transactional
public class EventoCanalJpaRepository implements EventoCanalRepository {

    private final EventoCanalJpaDataRepository dataRepository;
    private final CanalPersistenceMapper mapper;

    public EventoCanalJpaRepository(EventoCanalJpaDataRepository dataRepository, CanalPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public EventoCanal guardar(EventoCanal evento) {
        if (evento == null) {
            throw new IllegalArgumentException("El evento a guardar es obligatorio");
        }
        if (dataRepository.existsByEventoId(evento.getId())) {
            throw new IllegalStateException("La bitácora ya tiene el evento " + evento.getId());
        }
        return mapper.aDominio(dataRepository.save(mapper.aEntidad(evento)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoCanal> listarBitacora() {
        return dataRepository.findAllByOrderByFechaDescIdDesc().stream().map(mapper::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoCanal> listarPorCanalEIdentificador(CanalOrigen canal, String identificadorExterno) {
        if (canal == null || identificadorExterno == null) {
            return List.of();
        }
        return dataRepository.findByCanalOrigenAndIdentificadorExternoOrderByFechaDescIdDesc(canal,
                identificadorExterno.trim()).stream().map(mapper::aDominio).toList();
    }

    /** RN-19: qué cuenta como duplicado lo decide el dominio (EventoCanal.esDuplicado). */
    @Override
    @Transactional(readOnly = true)
    public boolean existeDuplicado(CanalOrigen canal, String identificadorExterno, TipoEventoCanal tipo) {
        return listarPorCanalEIdentificador(canal, identificadorExterno).stream()
                .anyMatch(e -> e.esDuplicado(canal, identificadorExterno, tipo));
    }
}