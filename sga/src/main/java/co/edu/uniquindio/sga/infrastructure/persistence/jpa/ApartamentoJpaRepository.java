package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.BloqueoEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ApartamentoPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador JPA del puerto ApartamentoRepository. Al guardar también guarda los bloqueos del agregado en la tabla
 * bloqueo (por su id, así no se duplican con los que guarda BloqueoRepository) al leer los vuelve a cargar.
 * No hay borrado físico: desactivar es una eliminación lógica que se persiste con guardar (7.3, 12.4).
 */
@Transactional
public class ApartamentoJpaRepository implements ApartamentoRepository {

    private final ApartamentoJpaDataRepository dataRepository;
    private final BloqueoJpaDataRepository bloqueoDataRepository;
    private final ApartamentoPersistenceMapper mapper;

    public ApartamentoJpaRepository(ApartamentoJpaDataRepository dataRepository,
                                    BloqueoJpaDataRepository bloqueoDataRepository,
                                    ApartamentoPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.bloqueoDataRepository = Objects.requireNonNull(bloqueoDataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Apartamento guardar(Apartamento apartamento) {
        if (apartamento == null) {
            throw new IllegalArgumentException("El apartamento a guardar es obligatorio");
        }
        apartamento.getBloqueos().forEach(this::guardarBloqueo);
        ApartamentoEntity entidad = mapper.aEntidad(apartamento);
        dataRepository.findByIdentificacion(apartamento.getIdentificacion().codigo())
                .ifPresent(existente -> entidad.setId(existente.getId()));
        return aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Apartamento> buscarPorIdentificacion(IdentificacionApartamento identificacion) {
        if (identificacion == null) {
            return Optional.empty();
        }
        return dataRepository.findByIdentificacion(identificacion.codigo()).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existe(IdentificacionApartamento identificacion) {
        return identificacion != null && dataRepository.existsByIdentificacion(identificacion.codigo());
    }

    /** 7.3: la regla de qué apartamento está a la venta sigue en el dominio (estaVigenteParaVenta). */
    @Override
    @Transactional(readOnly = true)
    public List<Apartamento> listarVigentesParaVenta() {
        return listarNoEliminados().stream().filter(Apartamento::estaVigenteParaVenta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Apartamento> listarNoEliminados() {
        return dataRepository.findByEliminadoFalseOrderByIdentificacionAsc().stream().map(this::aDominio).toList();
    }

    private void guardarBloqueo(Bloqueo bloqueo) {
        BloqueoEntity entidad = mapper.aEntidad(bloqueo);
        bloqueoDataRepository.findByBloqueoId(bloqueo.getId()).ifPresent(existente -> entidad.setId(existente.getId()));
        bloqueoDataRepository.save(entidad);
    }

    private Apartamento aDominio(ApartamentoEntity entidad) {
        return mapper.aDominio(entidad, bloqueoDataRepository.findByApartamentoIdOrderByIdAsc(entidad.getIdentificacion()));
    }
}