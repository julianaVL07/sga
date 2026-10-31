package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.util.List;

/**
 * CU-23 (7.3): resultado de actualizar un apartamento. Cambiar la capacidad no afecta las reservas ya creadas,
 * pero el sistema advierte cuáles reservas activas o futuras quedan por encima de la nueva capacidad.
 */
public record ResultadoActualizacionApartamento(Apartamento apartamento, List<CodigoReserva> reservasSobreCapacidad) {

    public ResultadoActualizacionApartamento {
        if (apartamento == null) {
            throw new ReglaDominioException("7.3: el resultado de la actualización exige el apartamento");
        }
        reservasSobreCapacidad = reservasSobreCapacidad == null ? List.of() : List.copyOf(reservasSobreCapacidad);
    }

    /** 7.3: hay reservas que superan la nueva capacidad y el administrador debe revisarlas. */
    public boolean tieneAdvertencia() {
        return !reservasSobreCapacidad.isEmpty();
    }
}
