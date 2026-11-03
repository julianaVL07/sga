package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * Política de mascotas y su cargo por aseo de mascota (L-16, glosario del equipo). Anexo A: se aceptan máximo
 * 2 mascotas de hasta 10 kg por apartamento, con un cargo único de $110.000 COP por estancia.
 */
public record CargoAseoMascota(boolean aplica, Dinero valor, int maximoMascotas, int pesoMaximoKg) {

    /** Concepto con el que el cargo de aseo queda en el folio; permite reconocerlo para revertirlo (RN-16). */
    public static final String CONCEPTO = "Cargo por aseo de mascota";

    public CargoAseoMascota {
        if (aplica) {
            if (valor == null || !valor.esPositivo()) {
                throw new ReglaDominioException("L-16: el cargo de aseo por mascota debe ser mayor que cero");
            }
            if (maximoMascotas < 1 || pesoMaximoKg < 1) {
                throw new ReglaDominioException("L-16: el límite de mascotas y su peso máximo deben ser positivos");
            }
        }
    }

    /** Política cuando el alojamiento no acepta mascotas. */
    public static CargoAseoMascota noSeAceptanMascotas() {
        return new CargoAseoMascota(false, Dinero.cero(), 0, 0);
    }

    /** L-16: verifica cantidad y peso de las mascotas del grupo. */
    public boolean admite(int cantidadMascotas, int pesoMayorKg) {
        if (cantidadMascotas == 0) {
            return true;
        }
        return aplica && cantidadMascotas <= maximoMascotas && pesoMayorKg <= pesoMaximoKg;
    }

    /** Cargo único por estancia que se asienta en el folio cuando el grupo trae mascotas. */
    public Cargo generarCargoServicio(LocalDate fechaActual) {
        if (!aplica) {
            throw new ReglaDominioException("L-16: el alojamiento no acepta mascotas");
        }
        return new Cargo(TipoCargo.SERVICIO_ADICIONAL, CONCEPTO, valor, fechaActual);
    }

    /** true si el cargo es un cargo de aseo de mascota asentado en el folio (no su movimiento inverso). */
    public static boolean esCargoDeAseo(Cargo cargo) {
        return cargo != null && cargo.tipo() == TipoCargo.SERVICIO_ADICIONAL && CONCEPTO.equals(cargo.concepto())
                && cargo.valor().esPositivo();
    }

    /** true si el cargo es el movimiento inverso de un cargo de aseo de mascota (RN-16). */
    public static boolean esReversoDeCargoDeAseo(Cargo cargo) {
        return cargo != null && cargo.tipo() == TipoCargo.SERVICIO_ADICIONAL
                && cargo.concepto().startsWith("Reverso de '" + CONCEPTO + "'") && cargo.valor().esNegativo();
    }
}
