package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TemporadaEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.AlojamientoPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adaptador JPA del puerto TemporadaRepository. Comparte la tabla temporada con AlojamientoJpaRepository. */
@Transactional
public class TemporadaJpaRepository implements TemporadaRepository {

    private final TemporadaJpaDataRepository dataRepository;
    private final AlojamientoPersistenceMapper mapper;

    public TemporadaJpaRepository(TemporadaJpaDataRepository dataRepository, AlojamientoPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Temporada guardar(Temporada temporada) {
        if (temporada == null) {
            throw new IllegalArgumentException("La temporada a guardar es obligatoria");
        }
        if (temporada.esBase()) {
            dataRepository.findFirstByTipo(TipoTemporada.BASE)
                    .filter(base -> !base.getTemporadaId().equals(temporada.getId()))
                    .ifPresent(base -> {
                        throw new ReglaDominioException("F-05: ya existe la temporada base " + base.getNombre());
                    });
        }
        TemporadaEntity entidad = mapper.aEntidad(temporada);
        dataRepository.findByTemporadaId(temporada.getId()).ifPresent(existente -> entidad.setId(existente.getId()));
        return mapper.aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Temporada> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return dataRepository.findByTemporadaId(id).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Temporada> buscarBase() {
        return dataRepository.findFirstByTipo(TipoTemporada.BASE).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Temporada> listarTodas() {
        return dataRepository.findAllByOrderByNombreAsc().stream().map(mapper::aDominio).toList();
    }
}