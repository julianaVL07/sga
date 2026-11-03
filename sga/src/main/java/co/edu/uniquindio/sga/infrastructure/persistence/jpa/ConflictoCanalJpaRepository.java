package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;
import co.edu.uniquindio.sga.domain.repository.ConflictoCanalRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ConflictoCanalEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.CanalPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adaptador JPA del puerto ConflictoCanalRepository (RN-18). */
@Transactional
public class ConflictoCanalJpaRepository implements ConflictoCanalRepository {

    private final ConflictoCanalJpaDataRepository dataRepository;
    private final CanalPersistenceMapper mapper;

    public ConflictoCanalJpaRepository(ConflictoCanalJpaDataRepository dataRepository, CanalPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public ConflictoCanal guardar(ConflictoCanal conflicto) {
        if (conflicto == null) {
            throw new IllegalArgumentException("El conflicto a guardar es obligatorio");
        }
        ConflictoCanalEntity entidad = mapper.aEntidad(conflicto);
        dataRepository.findByConflictoId(conflicto.getId()).ifPresent(existente -> entidad.setId(existente.getId()));
        return mapper.aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConflictoCanal> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return dataRepository.findByConflictoId(id).map(mapper::aDominio);
    }

    /** Qué conflicto sigue pendiente lo decide el dominio (ConflictoCanal.estaPendiente). */
    @Override
    @Transactional(readOnly = true)
    public List<ConflictoCanal> listarPendientes() {
        return listarTodos().stream().filter(ConflictoCanal::estaPendiente).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictoCanal> listarTodos() {
        return dataRepository.findAllByOrderByFechaDescIdDesc().stream().map(mapper::aDominio).toList();
    }
}
