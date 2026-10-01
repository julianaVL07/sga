package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de novedades (7.6). */
public interface NovedadRepository {

    Novedad guardar(Novedad novedad);

    Optional<Novedad> buscarPorId(String id);

    /** 7.6: historial de novedades de un apartamento, de la más reciente a la más antigua. */
    List<Novedad> listarPorApartamento(IdentificacionApartamento apartamento);

    /** Novedades de gravedad ALTA sin atender. */
    List<Novedad> listarQueRequierenAtencionInmediata();
}
