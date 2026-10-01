package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;

/**
 * Política de mascotas y su cargo por aseo de mascota (L-16, glosario del equipo). Anexo A: se aceptan máximo
 * 2 mascotas de hasta 10 kg por apartamento, con un cargo único de $110.000 COP por estancia.
 */
public record CargoAseoMascota(boolean aplica, Dinero valor, int maximoMascotas, int pesoMaximoKg) {

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
        return new Cargo(TipoCargo.SERVICIO_ADICIONAL, "Cargo por aseo de mascota", valor, fechaActual);
    }
}
