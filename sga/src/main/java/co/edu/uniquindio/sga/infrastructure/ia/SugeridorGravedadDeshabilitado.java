package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.port.SugeridorGravedad;
import co.edu.uniquindio.sga.domain.valueobject.SugerenciaGravedad;

import java.util.Optional;

/**
 * Adaptador del puerto SugeridorGravedad con la IA apagada (sga.ia.proveedor=deshabilitado): nunca sugiere nada.
 * El formulario de novedades funciona igual y la persona elige la gravedad sin ayuda (10.2).
 */
public class SugeridorGravedadDeshabilitado implements SugeridorGravedad {

    @Override
    public Optional<SugerenciaGravedad> sugerir(String descripcion) {
        return Optional.empty();
    }
}
