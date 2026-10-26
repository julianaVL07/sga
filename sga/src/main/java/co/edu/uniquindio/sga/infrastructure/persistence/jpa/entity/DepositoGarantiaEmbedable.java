package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Depósito de garantía del folio (RP-02). Sus columnas van en la misma tabla folio. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepositoGarantiaEmbedable {

    private String depositoConcepto;

    private BigDecimal depositoMonto;

    private boolean depositoReembolsado;
}