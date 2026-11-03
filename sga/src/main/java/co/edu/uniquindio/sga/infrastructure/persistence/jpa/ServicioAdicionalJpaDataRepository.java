package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ServicioAdicionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data para la tabla servicio_adicional. */
public interface ServicioAdicionalJpaDataRepository extends JpaRepository<ServicioAdicionalEntity, Long> {

    Optional<ServicioAdicionalEntity> findByServicioId(String servicioId);

    List<ServicioAdicionalEntity> findByActivoTrueOrderByNombreAsc();

    List<ServicioAdicionalEntity> findAllByOrderByNombreAsc();

    List<ServicioAdicionalEntity> findAllByOrderByIdAsc();
}
