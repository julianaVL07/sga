package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

import java.time.LocalDate;
import java.time.Period;

/**
 * Persona incluida en la reserva, con su fecha de nacimiento (glosario, 3.2).
 * La edad se calcula, nunca se almacena. El documento es opcional (por ejemplo, un bebé);
 * sirve para reconocer al titular dentro del grupo.
 *
 * Reglas: RN-06 (es facturable si a la fecha de entrada alcanza el umbral), RN-02 (cuenta para la capacidad).
 */
public record Ocupante(String nombre, DocumentoIdentidad documento, LocalDate fechaNacimiento) {

    public Ocupante {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("3.2: el nombre del ocupante es obligatorio");
        }
        if (fechaNacimiento == null) {
            throw new ReglaDominioException("3.2: de cada ocupante se registra su fecha de nacimiento");
        }
        nombre = nombre.trim();
    }

    /** Edad cumplida a una fecha de referencia. */
    public int edadA(LocalDate fechaReferencia) {
        if (fechaReferencia == null) {
            throw new ReglaDominioException("3.2: la fecha de referencia es obligatoria");
        }
        if (fechaReferencia.isBefore(fechaNacimiento)) {
            throw new ReglaDominioException("3.2: el ocupante no ha nacido en la fecha " + fechaReferencia);
        }
        return Period.between(fechaNacimiento, fechaReferencia).getYears();
    }

    /**
     * RN-06 y 3.2: se evalúa a la fecha de entrada. Quien cumple años durante la estancia no cambia de condición.
     */
    public boolean esFacturableEn(Estancia estancia, UmbralEdadFacturable umbral) {
        if (estancia == null || umbral == null) {
            throw new ReglaDominioException("RN-06: la estancia y el umbral son obligatorios");
        }
        return umbral.esFacturable(edadA(estancia.fechaEntrada()));
    }

    public boolean tieneDocumento(DocumentoIdentidad otro) {
        return documento != null && documento.equals(otro);
    }
}
