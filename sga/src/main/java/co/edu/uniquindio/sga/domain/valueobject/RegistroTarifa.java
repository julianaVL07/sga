package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * CU-24 (7.4): una entrada del histórico de tarifas de un apartamento en una temporada.
 * vigenteDesde es el día en que se definió la tarifa; vigenteHasta, el día en que se reemplazó.
 * Mientras la tarifa siga vigente vigenteHasta es null; al reemplazarla se crea un registro nuevo con
 * {@link #reemplazadaEl(LocalDate)}, porque el objeto de valor es inmutable.
 * vigenteDesde puede ser null solo para tarifas cargadas sin fecha (datos iniciales anteriores al histórico).
 * La cotización no usa este registro: siempre usa la tarifa vigente del apartamento (RN-05).
 */
public record RegistroTarifa(String idTemporada, Dinero valorPorOcupante, LocalDate vigenteDesde,
                             LocalDate vigenteHasta) {

    public RegistroTarifa {
        if (idTemporada == null || idTemporada.isBlank()) {
            throw new ReglaDominioException("7.4: el registro de tarifa debe indicar la temporada");
        }
        if (valorPorOcupante == null || !valorPorOcupante.esPositivo()) {
            throw new ReglaDominioException("7.4: el valor de la tarifa debe ser mayor que cero");
        }
        if (vigenteDesde != null && vigenteHasta != null && vigenteHasta.isBefore(vigenteDesde)) {
            throw new ReglaDominioException("7.4: una tarifa no puede reemplazarse antes de la fecha desde la que "
                    + "estuvo vigente");
        }
    }

    /** Registro de una tarifa que empieza a regir en la fecha dada. */
    public static RegistroTarifa vigenteDesde(Tarifa tarifa, LocalDate desde) {
        if (tarifa == null) throw new ReglaDominioException("La tarifa es obligatoria");
        if (desde == null) throw new ReglaDominioException("7.4: la fecha desde la que rige la tarifa es obligatoria");
        return new RegistroTarifa(tarifa.idTemporada(), tarifa.valorPorOcupante(), desde, null);
    }

    /** El mismo registro, cerrado en la fecha en que otra tarifa lo reemplazó. */
    public RegistroTarifa reemplazadaEl(LocalDate fecha) {
        if (fecha == null) throw new ReglaDominioException("7.4: la fecha de reemplazo es obligatoria");
        if (!estaVigente()) {
            throw new ReglaDominioException("7.4: la tarifa de la temporada " + idTemporada + " ya fue reemplazada");
        }
        return new RegistroTarifa(idTemporada, valorPorOcupante, vigenteDesde, fecha);
    }

    public boolean estaVigente() {
        return vigenteHasta == null;
    }

    public boolean esDeTemporada(String temporada) {
        return idTemporada.equals(temporada);
    }
}
