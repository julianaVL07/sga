package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.MedioPago;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un pago del folio con su medio y su fecha (RN-15), fila de la tabla hija folio_pago. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoEmbedable {

    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedioPago medio;

    @Column(nullable = false)
    private LocalDate fecha;
}