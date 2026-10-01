package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.ConflictoCanal;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de conflictos de canal (RN-18). */
public interface ConflictoCanalRepository {

    ConflictoCanal guardar(ConflictoCanal conflicto);

    Optional<ConflictoCanal> buscarPorId(String id);

    /** Conflictos aún no revisados por el administrador, del más reciente al más antiguo. */
    List<ConflictoCanal> listarPendientes();

    List<ConflictoCanal> listarTodos();
}
