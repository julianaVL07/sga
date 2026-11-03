package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.PoliticaCancelacionPersistenceMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/** Adaptador JPA del puerto PoliticaCancelacionRepository. Solo inserta versiones nuevas: nunca actualiza una. */
@Transactional
public class PoliticaCancelacionJpaRepository implements PoliticaCancelacionRepository {

    private final PoliticaCancelacionJpaDataRepository dataRepository;
    private final PoliticaCancelacionPersistenceMapper mapper;

    public PoliticaCancelacionJpaRepository(PoliticaCancelacionJpaDataRepository dataRepository,
                                            PoliticaCancelacionPersistenceMapper mapper) {
        this.dataRepository = Objects.requireNonNull(dataRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public PoliticaCancelacion guardar(PoliticaCancelacion politica) {
        if (politica == null) {
            throw new IllegalArgumentException("La política a guardar es obligatoria");
        }
        if (dataRepository.existsByVersion(politica.version())) {
            throw new ReglaDominioException("F-07: La versión " + politica.version()
                    + " de la política de cancelación ya existe y no se modifica");
        }
        return mapper.aDominio(dataRepository.save(mapper.aEntidad(politica)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PoliticaCancelacion> obtenerVigente() {
        return dataRepository.findFirstByOrderByVersionDesc().map(mapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<PoliticaCancelacion> listarVersiones(int pagina, int tamano) {
        return mapper.aPagina(dataRepository.findAllByOrderByVersionDesc(PageRequest.of(pagina, tamano)));
    }
}
