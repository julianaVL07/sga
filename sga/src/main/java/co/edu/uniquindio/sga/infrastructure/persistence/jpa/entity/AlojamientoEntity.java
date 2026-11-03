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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de la tabla alojamiento (Guía 09, 6.7). Los parámetros del Anexo A van como columnas; los objetos de valor
 * pequeños (ubicación, estancia mínima, política de mascotas) se aplanan en columnas propias.
 * De sus apartamentos solo guarda los ids (alojamiento_apartamento): entre agregados solo viaja la identificación.
 * Temporadas y servicios adicionales tienen su propia tabla y su propio repositorio.
 */
@Entity
@Table(name = "alojamiento")
@Getter
@Setter
@NoArgsConstructor
public class AlojamientoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String alojamientoId;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private String ciudad;

    private String direccion;

    private double latitud;
    private double longitud;

    private LocalTime horaEntrada;
    private LocalTime horaSalida;

    @Column(length = 1000)
    private String normaConvivencia;

    // Parámetros del Anexo A (L-09 a L-15); quedan en null hasta que se configuran
    private Integer umbralEdadFacturable;
    private Integer tiempoPreparacionHoras;
    private Integer plazoConfirmacionHoras;
    private LocalTime horaLimiteNoShow;
    private LocalTime horaInicioLlegadaNocturna;
    private Integer estanciaMinimaNoches;                // RP-01
    private boolean estanciaMinimaAplicaTemporadaAltaMedia;
    private boolean exigeAnticipo;                       // L-11
    private int porcentajeAnticipo;

    // Política de mascotas (L-16)
    private boolean mascotasAplica;
    private BigDecimal mascotasValor;
    private int mascotasMaximo;
    private int mascotasPesoMaximoKg;

    @ElementCollection
    @CollectionTable(name = "alojamiento_apartamento", joinColumns = @JoinColumn(name = "alojamiento_id"))
    @Column(name = "apartamento_id", nullable = false)
    @OrderColumn(name = "posicion")
    private List<String> apartamentos = new ArrayList<>();
}