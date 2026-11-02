package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de la tabla politica_cancelacion (CU-30, F-07, 7.4): una fila por versión. Las versiones solo se agregan;
 * la vigente es la de mayor número. Sus tramos van en la tabla hija politica_cancelacion_tramo con el mismo
 * Embeddable que usa la copia congelada en cada reserva (RN-22).
 */
@Entity
@Table(name = "politica_cancelacion")
@Getter
@Setter
@NoArgsConstructor
public class PoliticaCancelacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_version", nullable = false, unique = true)
    private int version;

    @Column(nullable = false)
    private LocalDate fechaVigencia;

    @Column(nullable = false, length = 1000)
    private String consecuenciaNoShow;

    @ElementCollection
    @CollectionTable(name = "politica_cancelacion_tramo", joinColumns = @JoinColumn(name = "politica_id"))
    @OrderColumn(name = "posicion")
    private List<TramoCancelacionEmbedable> tramos = new ArrayList<>();
}
