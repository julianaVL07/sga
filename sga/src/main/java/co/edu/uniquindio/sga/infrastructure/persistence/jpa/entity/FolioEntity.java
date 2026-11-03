package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de la tabla folio (7.7). La reserva se guarda por su código ("RES-2026-00001") y no como relación JPA:
 * entre agregados solo viaja el id. Un folio por reserva, por eso el código es único.
 * Cargos y pagos van en tablas hijas, en el orden en que se registraron, y se quedan LAZY.
 */
@Entity
@Table(name = "folio", uniqueConstraints = {
        @UniqueConstraint(name = "uk_folio_folio_id", columnNames = "folio_id"),
        @UniqueConstraint(name = "uk_folio_codigo_reserva", columnNames = "codigo_reserva")
})
@Getter
@Setter
@NoArgsConstructor
public class FolioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio_id", nullable = false)
    private String folioId;                              // FOL-RES-YYYY-NNNNN

    @Column(name = "codigo_reserva", nullable = false)
    private String codigoReserva;

    @Embedded
    private DepositoGarantiaEmbedable depositoGarantia;  // RP-02

    @Column(nullable = false)
    private LocalDate fechaApertura;

    @ElementCollection
    @CollectionTable(name = "folio_cargo", joinColumns = @JoinColumn(name = "folio_id"))
    @OrderColumn(name = "posicion")
    private List<CargoEmbedable> cargos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "folio_pago", joinColumns = @JoinColumn(name = "folio_id"))
    @OrderColumn(name = "posicion")
    private List<PagoEmbedable> pagos = new ArrayList<>();

    private boolean cerrado;

    private LocalDate fechaCierre;

    @Embedded
    private AutorizacionCierreEmbedable autorizacionCierre;   // RN-17
}