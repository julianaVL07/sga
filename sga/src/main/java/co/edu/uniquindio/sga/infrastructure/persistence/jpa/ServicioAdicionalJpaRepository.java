package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ServicioAdicionalEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.AlojamientoPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adaptador JPA del puerto ServicioAdicionalRepository. Comparte la tabla con AlojamientoJpaRepository. */
@Transactional
public class ServicioAdicionalJpaRepository implements ServicioAdicionalRepository {

    private final ServicioAdicionalJpaDataRepository dataRepository;
    private final AlojamientoPersistenceMapper mapper;

    public ServicioAdicionalJpaRepository(ServicioAdicionalJpaDataRepository dataRepository,
                                          AlojamientoPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public ServicioAdicional guardar(ServicioAdicional servicio) {
        if (servicio == null) {
            throw new IllegalArgumentException("El servicio a guardar es obligatorio");
        }
        ServicioAdicionalEntity entidad = mapper.aEntidad(servicio);
        dataRepository.findByServicioId(servicio.getId()).ifPresent(existente -> entidad.setId(existente.getId()));
        return mapper.aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServicioAdicional> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return dataRepository.findByServicioId(id).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioAdicional> listarActivos() {
        return dataRepository.findByActivoTrueOrderByNombreAsc().stream().map(mapper::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioAdicional> listarTodos() {
        return dataRepository.findAllByOrderByNombreAsc().stream().map(mapper::aDominio).toList();
    }
}