package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.AlojamientoEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ServicioAdicionalEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TemporadaEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.AlojamientoPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador JPA del puerto AlojamientoRepository. Guarda el alojamiento y, en la misma transacción, sus temporadas
 * y servicios adicionales en sus tablas, al leer los vuelve a juntar en el agregado.
 */
@Transactional
public class AlojamientoJpaRepository implements AlojamientoRepository {

    private final AlojamientoJpaDataRepository dataRepository;
    private final TemporadaJpaDataRepository temporadaDataRepository;
    private final ServicioAdicionalJpaDataRepository servicioDataRepository;
    private final AlojamientoPersistenceMapper mapper;

    public AlojamientoJpaRepository(AlojamientoJpaDataRepository dataRepository,
                                    TemporadaJpaDataRepository temporadaDataRepository,
                                    ServicioAdicionalJpaDataRepository servicioDataRepository,
                                    AlojamientoPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.temporadaDataRepository = Objects.requireNonNull(temporadaDataRepository);
        this.servicioDataRepository = Objects.requireNonNull(servicioDataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Alojamiento guardar(Alojamiento alojamiento) {
        if (alojamiento == null) {
            throw new IllegalArgumentException("El alojamiento a guardar es obligatorio");
        }
        dataRepository.findFirstByOrderByIdAsc()
                .filter(existente -> !existente.getAlojamientoId().equals(alojamiento.getId()))
                .ifPresent(existente -> {
                    throw new ReglaDominioException("F-13: el sistema administra un único alojamiento");
                });

        alojamiento.getTemporadas().forEach(this::guardarTemporada);
        alojamiento.getServiciosAdicionales().forEach(this::guardarServicio);

        AlojamientoEntity entidad = mapper.aEntidad(alojamiento);
        dataRepository.findByAlojamientoId(alojamiento.getId())
                .ifPresent(existente -> entidad.setId(existente.getId()));
        return aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alojamiento> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return dataRepository.findByAlojamientoId(id).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alojamiento> obtener() {
        return dataRepository.findFirstByOrderByIdAsc().map(this::aDominio);
    }

    private void guardarTemporada(Temporada temporada) {
        TemporadaEntity entidad = mapper.aEntidad(temporada);
        temporadaDataRepository.findByTemporadaId(temporada.getId())
                .ifPresent(existente -> entidad.setId(existente.getId()));
        temporadaDataRepository.save(entidad);
    }

    private void guardarServicio(ServicioAdicional servicio) {
        ServicioAdicionalEntity entidad = mapper.aEntidad(servicio);
        servicioDataRepository.findByServicioId(servicio.getId())
                .ifPresent(existente -> entidad.setId(existente.getId()));
        servicioDataRepository.save(entidad);
    }

    private Alojamiento aDominio(AlojamientoEntity entidad) {
        return mapper.aDominio(entidad, temporadaDataRepository.findAllByOrderByIdAsc(),
                servicioDataRepository.findAllByOrderByIdAsc());
    }
}