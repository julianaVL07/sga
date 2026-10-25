package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.BloqueoEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ApartamentoPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adaptador JPA del puerto BloqueoRepository. Comparte la tabla bloqueo con ApartamentoJpaRepository. */
@Transactional
public class BloqueoJpaRepository implements BloqueoRepository {

    private final BloqueoJpaDataRepository dataRepository;
    private final ApartamentoPersistenceMapper mapper;

    public BloqueoJpaRepository(BloqueoJpaDataRepository dataRepository, ApartamentoPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Bloqueo guardar(Bloqueo bloqueo) {
        if (bloqueo == null) {
            throw new IllegalArgumentException("El bloqueo a guardar es obligatorio");
        }
        BloqueoEntity entidad = mapper.aEntidad(bloqueo);
        dataRepository.findByBloqueoId(bloqueo.getId()).ifPresent(existente -> entidad.setId(existente.getId()));
        return mapper.aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bloqueo> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return dataRepository.findByBloqueoId(id).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bloqueo> listarVigentesPorApartamento(IdentificacionApartamento apartamento) {
        return dataRepository.findByApartamentoIdAndVigenteTrueOrderByRangoFechaInicioAsc(apartamento.codigo())
                .stream().map(mapper::aDominio).toList();
    }

    /** RN-07: el cruce de fechas lo decide el dominio (Bloqueo.seSolapaCon). */
    @Override
    @Transactional(readOnly = true)
    public List<Bloqueo> listarVigentesQueSeSolapan(IdentificacionApartamento apartamento, Estancia estancia) {
        return listarVigentesPorApartamento(apartamento).stream().filter(b -> b.seSolapaCon(estancia)).toList();
    }
}