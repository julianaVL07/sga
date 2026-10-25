package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
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

/** Fila de la tabla novedad (7.6): daño, faltante o incidencia de un apartamento. */
@Entity
@Table(name = "novedad")
@Getter
@Setter
@NoArgsConstructor
public class NovedadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String novedadId;

    @Column(nullable = false)
    private String apartamentoId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String autor;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GravedadNovedad gravedad;

    private boolean atendida;

    private LocalDate fechaAtencion;
}