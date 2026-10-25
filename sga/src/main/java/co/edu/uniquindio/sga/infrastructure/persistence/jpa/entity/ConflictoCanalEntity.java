package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Fila de la tabla conflicto_canal (RN-18): reserva externa rechazada, pendiente de revisión del administrador. */
@Entity
@Table(name = "conflicto_canal")
@Getter
@Setter
@NoArgsConstructor
public class ConflictoCanalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String conflictoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalOrigen canalOrigen;

    @Column(nullable = false)
    private String identificadorExterno;

    @Column(nullable = false, length = 1000)
    private String detalle;

    @Column(nullable = false)
    private LocalDate fecha;

    private boolean revisado;

    @Column(length = 1000)
    private String notasAdministrador;

    private LocalDate fechaRevision;
}
