package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Tarifa por ocupante facturable del apartamento en una temporada (7.4), fila de apartamento_tarifa. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarifaEmbedable {

    @Column(nullable = false)
    private String idTemporada;

    @Column(nullable = false)
    private BigDecimal valorPorOcupante;
}