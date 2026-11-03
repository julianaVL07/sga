package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.TipoCargo;
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

/**
 * Un cargo del folio, fila de la tabla hija folio_cargo (7.7). Puede ser negativo: ajuste a la baja (RN-14)
 * o movimiento inverso (RN-16). Nunca se edita ni se borra: el folio solo agrega filas.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CargoEmbedable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCargo tipo;

    @Column(nullable = false, length = 500)
    private String concepto;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate fecha;
}