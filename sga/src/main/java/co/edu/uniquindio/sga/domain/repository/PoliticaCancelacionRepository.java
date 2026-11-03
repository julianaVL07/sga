package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;

import java.util.Optional;

/**
 * Puerto de persistencia de la política de cancelación versionada (F-07, 7.4). Solo se agregan versiones: una
 * versión guardada nunca se modifica ni se borra, porque las reservas que la congelaron la siguen usando (RN-22).
 */
public interface PoliticaCancelacionRepository {

    /** Guarda una versión nueva. Si ya existe una versión con el mismo número, la rechaza. */
    PoliticaCancelacion guardar(PoliticaCancelacion politica);

    /** 7.4: la única versión vigente, que es la de mayor número; vacío si aún no hay ninguna. */
    Optional<PoliticaCancelacion> obtenerVigente();

    /**
     * CU-30 (12.4): todas las versiones, de la más reciente a la más antigua, una página a la vez.
     * La numeración de páginas empieza en 0.
     */
    Pagina<PoliticaCancelacion> listarVersiones(int pagina, int tamano);
}
