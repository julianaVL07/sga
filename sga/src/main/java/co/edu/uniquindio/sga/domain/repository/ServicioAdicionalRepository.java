package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de servicios adicionales (L-17). */
public interface ServicioAdicionalRepository {

    ServicioAdicional guardar(ServicioAdicional servicio);

    Optional<ServicioAdicional> buscarPorId(String id);

    List<ServicioAdicional> listarActivos();

    List<ServicioAdicional> listarTodos();
}
