package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;

import java.util.Optional;

/**
 * Puerto de persistencia del alojamiento. El sistema administra un único alojamiento (F-13).
 */
public interface AlojamientoRepository {

    Alojamiento guardar(Alojamiento alojamiento);

    Optional<Alojamiento> buscarPorId(String id);

    /** F-13: devuelve el único alojamiento configurado, si ya existe. */
    Optional<Alojamiento> obtener();
}
