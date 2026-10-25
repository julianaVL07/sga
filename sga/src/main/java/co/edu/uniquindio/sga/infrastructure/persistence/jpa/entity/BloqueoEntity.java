package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Fila de la tabla bloqueo (7.3, RN-07). Un bloqueo levantado se conserva con vigente = false. */
@Entity
@Table(name = "bloqueo")
@Getter
@Setter
@NoArgsConstructor
public class BloqueoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bloqueoId;

    @Column(nullable = false)
    private String apartamentoId;

    @Embedded
    private RangoFechasEmbedable rango;

    @Column(nullable = false)
    private String motivo;

    private boolean vigente;

    private LocalDate fechaLevantamiento;
}