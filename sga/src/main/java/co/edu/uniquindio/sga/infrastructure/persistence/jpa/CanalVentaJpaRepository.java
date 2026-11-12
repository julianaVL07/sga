package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CanalVentaEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.CanalPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/** Adaptador JPA del puerto CanalVentaRepository (10.3.1). Nunca borra filas: desactivar es una actualización. */
@Transactional
public class CanalVentaJpaRepository implements CanalVentaRepository {

    private final CanalVentaJpaDataRepository dataRepository;
    private final CanalPersistenceMapper mapper;

    public CanalVentaJpaRepository(CanalVentaJpaDataRepository dataRepository, CanalPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public CanalVenta guardar(CanalVenta canal) {
        if (canal == null) {
            throw new IllegalArgumentException("El canal a guardar es obligatorio");
        }
        CanalVentaEntity entidad = mapper.aEntidad(canal);
        dataRepository.findByNombre(canal.getNombre()).ifPresent(existente -> entidad.setId(existente.getId()));
        return mapper.aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CanalVenta> buscarPorNombre(String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        return dataRepository.findByNombre(nombre).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNombre(String nombre) {
        return nombre != null && dataRepository.existsByNombre(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<CanalVenta> listar(int pagina, int tamano) {
        if (pagina < 0 || tamano <= 0) {
            throw new ReglaDominioException("12.4: la página no puede ser negativa y el tamaño debe ser mayor que cero");
        }
        Page<CanalVentaEntity> resultado = dataRepository.findAllBy(
                PageRequest.of(pagina, tamano, Sort.by("nombre").ascending()));
        return new Pagina<>(resultado.getContent().stream().map(mapper::aDominio).toList(),
                pagina, tamano, resultado.getTotalElements());
    }
}
