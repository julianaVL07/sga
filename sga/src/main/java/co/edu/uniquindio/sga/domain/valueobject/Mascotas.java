package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Mascotas que trae el grupo de la reserva (L-16): cuántas son y el peso de la más pesada, en kg.
 * Una reserva sin mascotas tiene cantidad 0 y peso 0.
 */
public record Mascotas(int cantidad, int pesoMayorKg) {

    public Mascotas {
        if (cantidad < 0 || pesoMayorKg < 0) {
            throw new ReglaDominioException("L-16: la cantidad y el peso de las mascotas no pueden ser negativos");
        }
        if (cantidad == 0 && pesoMayorKg > 0) {
            throw new ReglaDominioException("L-16: se indicó el peso de una mascota pero la cantidad de mascotas es 0");
        }
        if (cantidad > 0 && pesoMayorKg == 0) {
            throw new ReglaDominioException("L-16: debe indicarse el peso de la mascota más pesada");
        }
    }

    public static Mascotas ninguna() {
        return new Mascotas(0, 0);
    }

    public boolean traeMascotas() {
        return cantidad > 0;
    }

    /** L-16: rechaza las mascotas que la política del alojamiento no admite (cantidad o peso). */
    public void validarContra(CargoAseoMascota politica) {
        if (politica == null) {
            throw new ReglaDominioException("L-16: la política de mascotas es obligatoria");
        }
        if (politica.admite(cantidad, pesoMayorKg)) {
            return;
        }
        if (!politica.aplica()) {
            throw new ReglaDominioException("L-16: El alojamiento no acepta mascotas");
        }
        throw new ReglaDominioException("L-16: Se aceptan máximo " + politica.maximoMascotas()
                + " mascotas de hasta " + politica.pesoMaximoKg() + " kg (se recibieron " + cantidad
                + " con la más pesada de " + pesoMayorKg + " kg)");
    }
}
