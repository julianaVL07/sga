package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.FolioEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.FolioPersistenceMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador JPA del puerto FolioRepository (7.7). Cada guardar reescribe las tablas hijas con la lista completa
 * del agregado; como el dominio solo agrega cargos y pagos (RN-16), las filas anteriores quedan iguales.
 */
@Transactional
public class FolioJpaRepository implements FolioRepository {

    private final FolioJpaDataRepository dataRepository;
    private final FolioPersistenceMapper mapper;

    public FolioJpaRepository(FolioJpaDataRepository dataRepository, FolioPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Folio guardar(Folio folio) {
        if (folio == null) {
            throw new IllegalArgumentException("El folio a guardar es obligatorio");
        }
        dataRepository.findByCodigoReserva(folio.getReserva().valor())
                .filter(existente -> !existente.getFolioId().equals(folio.getId()))
                .ifPresent(existente -> {
                    throw new ReglaDominioException("7.7: la reserva " + folio.getReserva().valor() + " ya tiene un folio");
                });
        FolioEntity entidad = mapper.aEntidad(folio);
        dataRepository.findByFolioId(folio.getId()).ifPresent(existente -> entidad.setId(existente.getId()));
        return mapper.aDominio(dataRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folio> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return dataRepository.findByFolioId(id).map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Folio> buscarPorReserva(CodigoReserva codigoReserva) {
        if (codigoReserva == null) {
            return Optional.empty();
        }
        return dataRepository.findByCodigoReserva(codigoReserva.valor()).map(mapper::aDominio);
    }
}