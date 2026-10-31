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
 * CU-24 (7.4): una fila del histórico de tarifas, en apartamento_tarifa_historico.
 * vigenteHasta es null mientras la tarifa sigue vigente; vigenteDesde es null solo para tarifas cargadas sin fecha.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroTarifaEmbedable {

    @Column(nullable = false)
    private String idTemporada;

    @Column(nullable = false)
    private BigDecimal valorPorOcupante;

    private LocalDate vigenteDesde;

    private LocalDate vigenteHasta;
}
