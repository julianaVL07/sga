package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CodigoRecuperacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositorio Spring Data de los códigos para restablecer la contraseña (7.2). */
public interface CodigoRecuperacionJpaDataRepository extends JpaRepository<CodigoRecuperacionEntity, Long> {

    Optional<CodigoRecuperacionEntity> findByCorreo(String correo);
}
