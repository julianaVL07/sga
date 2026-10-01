package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.util.regex.Pattern;

/**
 * Identidad única e inmutable de una reserva (glosario: "Código de reserva").
 * Formato RES-YYYY-NNNNN: año de creación y secuencia incremental de cinco dígitos. Ejemplo: RES-2026-00042.
 */
public record CodigoReserva(String valor) {

    private static final Pattern FORMATO = Pattern.compile("RES-\\d{4}-\\d{5}");

    public CodigoReserva {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("El código de la reserva es obligatorio");
        }
        valor = valor.trim().toUpperCase();
        if (!FORMATO.matcher(valor).matches()) {
            throw new ReglaDominioException("El código de la reserva debe tener el formato RES-YYYY-NNNNN: " + valor);
        }
    }

    /** Código para la reserva número {@code secuencia} del año {@code anio}. */
    public static CodigoReserva generar(int anio, int secuencia) {
        if (secuencia < 1 || secuencia > 99_999) {
            throw new ReglaDominioException("La secuencia del código de reserva debe estar entre 1 y 99999");
        }
        return new CodigoReserva(String.format("RES-%04d-%05d", anio, secuencia));
    }

    public int anio() {
        return Integer.parseInt(valor.substring(4, 8));
    }

    public int secuencia() {
        return Integer.parseInt(valor.substring(9));
    }
}
