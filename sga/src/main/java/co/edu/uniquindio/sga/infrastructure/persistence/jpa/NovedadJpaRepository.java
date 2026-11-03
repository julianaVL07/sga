package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.NovedadEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.NovedadPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adaptador JPA del puerto NovedadRepository (7.6). */
@Transactional
public class NovedadJpaRepository implements NovedadRepository {

    private final NovedadJpaDataRepository dataRepository;
    private final NovedadPersistenceMapper mapper;

    public NovedadJpaRepository(NovedadJpaDataRepository dataRepository, NovedadPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Novedad guardar(Novedad novedad) {
        if (novedad == null) {
            throw new IllegalArgumentException("La novedad a guardar es obligatoria");
        }
        NovedadEntity entidad = mapper.aEntidad(novedad);
        dataRepository.findByNovedadId(novedad.getId()).ifPresent(existente -> entidad.setId(existente.getId()));
        return mapper.aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Novedad> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return dataRepository.findByNovedadId(id).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novedad> listarPorApartamento(IdentificacionApartamento apartamento) {
        return dataRepository.findByApartamentoIdOrderByFechaDescIdDesc(apartamento.codigo())
                .stream().map(mapper::aDominio).toList();
    }

    /** 7.6: la página la arma la base de datos, de la más reciente a la más antigua (fecha y orden de registro). */
    @Override
    @Transactional(readOnly = true)
    public Pagina<Novedad> listarPorApartamento(IdentificacionApartamento apartamento, int pagina, int tamano) {
        if (pagina < 0 || tamano <= 0) {
            throw new ReglaDominioException("12.4: la página no puede ser negativa y el tamaño debe ser mayor que cero");
        }
        Page<NovedadEntity> resultado = dataRepository.findByApartamentoId(apartamento.codigo(),
                PageRequest.of(pagina, tamano, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))));
        return new Pagina<>(resultado.getContent().stream().map(mapper::aDominio).toList(),
                pagina, tamano, resultado.getTotalElements());
    }

    /** La regla (ALTA y sin atender) sigue en el dominio: Novedad.requiereAtencionInmediata. */
    @Override
    @Transactional(readOnly = true)
    public List<Novedad> listarQueRequierenAtencionInmediata() {
        return dataRepository.findByAtendidaFalseOrderByFechaDescIdDesc().stream()
                .map(mapper::aDominio)
                .filter(Novedad::requiereAtencionInmediata)
                .toList();
    }
}