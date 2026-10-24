package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una noche de la cotización congelada (RN-05, RN-22), fila de la tabla hija reserva_detalle_noche.
 * Se guarda tal como se calculó: si mañana cambia la tarifa, la reserva conserva su valor.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleNocheEmbedable {

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String temporada;

    @Column(nullable = false)
    private BigDecimal tarifaAplicada;

    private int ocupantesFacturables;

    @Column(nullable = false)
    private BigDecimal subtotal;
}