package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.UsuarioEntity;
import co.edu.uniquindio.sga.infrastructure.security.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Repositorio Spring Data de usuarios (Guía 12) */
public interface UsuarioJpaDataRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByDocumento(String documento);

    /** Listado del administrador (7.2): rol y activo son opcionales, un null no filtra */
    @Query("select u from UsuarioEntity u where (:rol is null or u.rol = :rol) and (:activo is null or u.activo = :activo)")
    Page<UsuarioEntity> buscar(RolUsuario rol, Boolean activo, Pageable pageable);
}
