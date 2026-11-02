package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Mascotas de la reserva (L-16) guardadas como dos columnas de la tabla reserva (Guía 09, 6.2).
 * Son Integer y admiten null para que las filas guardadas antes de L-16 se lean como "sin mascotas".
 */
@Embeddable
public record MascotasEmbedable(
        @Column(name = "mascotas_cantidad")
        Integer cantidad,

        @Column(name = "mascotas_peso_mayor_kg")
        Integer pesoMayorKg
) {
}
