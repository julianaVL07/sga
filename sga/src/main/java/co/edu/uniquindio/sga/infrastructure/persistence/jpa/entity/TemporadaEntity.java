package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila de la tabla temporada (F-05, 7.4). Pertenece al único alojamiento (F-13), por eso no guarda su id.
 * La temporada base no tiene rangos: cubre todas las fechas que no son Media ni Alta.
 */
@Entity
@Table(name = "temporada")
@Getter
@Setter
@NoArgsConstructor
public class TemporadaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String temporadaId;                          // T-BASE, T-MEDIA, T-ALTA

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTemporada tipo;

    @ElementCollection
    @CollectionTable(name = "temporada_rango", joinColumns = @JoinColumn(name = "temporada_id"))
    @OrderColumn(name = "posicion")
    private List<RangoFechasEmbedable> rangos = new ArrayList<>();
}