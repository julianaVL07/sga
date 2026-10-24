package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

/**
 * Estancia de la reserva guardada como dos columnas de la tabla reserva (Guía 09, 6.2).
 * Es un objeto de valor: no tiene @Id ni tabla propia. Hibernate 7 acepta records como @Embeddable.
 */
@Embeddable
public record EstanciaEmbedable(
        @Column(name = "fecha_entrada", nullable = false)
        LocalDate fechaEntrada,

        @Column(name = "fecha_salida", nullable = false)
        LocalDate fechaSalida
) {
}
