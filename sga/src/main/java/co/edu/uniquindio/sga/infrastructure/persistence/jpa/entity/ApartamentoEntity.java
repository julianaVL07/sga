package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
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
 * Fila de la tabla apartamento. Dotación, imágenes y tarifas son objetos de valor y van en tablas hijas.
 * Los bloqueos viven en la tabla bloqueo, que también usa BloqueoRepository: así un bloqueo se guarda una sola vez
 * y el adaptador de apartamentos los lee de ahí al reconstruir el agregado.
 */
@Entity
@Table(name = "apartamento")
@Getter
@Setter
@NoArgsConstructor
public class ApartamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String identificacion;                       // APT-101

    @Column(nullable = false)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    private int dormitorios;

    private int capacidadMaxima;

    @ElementCollection
    @CollectionTable(name = "apartamento_dotacion", joinColumns = @JoinColumn(name = "apartamento_id"))
    @Column(name = "elemento", nullable = false)
    @OrderColumn(name = "posicion")
    private List<String> dotacion = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "apartamento_imagen", joinColumns = @JoinColumn(name = "apartamento_id"))
    @OrderColumn(name = "posicion")
    private List<ImagenEmbedable> imagenes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOperativo estadoOperativo;

    private boolean activo;

    private boolean eliminado;                           // eliminación lógica (7.3, 12.4)

    @ElementCollection
    @CollectionTable(name = "apartamento_tarifa", joinColumns = @JoinColumn(name = "apartamento_id"))
    @OrderColumn(name = "posicion")
    private List<TarifaEmbedable> tarifas = new ArrayList<>();
}