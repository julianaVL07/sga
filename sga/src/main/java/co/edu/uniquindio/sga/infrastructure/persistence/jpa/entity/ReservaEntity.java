package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de la tabla reserva (Guía 09, 6.3). No es la entidad del dominio: Reserva protege las reglas y esta clase
 * solo existe para JPA (constructor vacío, setters e id técnico). ReservaPersistenceMapper traduce entre las dos.
 * El apartamento se guarda como texto ("APT-101") y no como relación JPA: entre agregados solo viaja el id.
 * Las tres listas se quedan LAZY; se leen dentro de la transacción del adaptador.
 */
@Entity
@Table(name = "reserva", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reserva_codigo", columnNames = "codigo"),
        @UniqueConstraint(name = "uk_reserva_canal_externo", columnNames = {"canal_origen", "identificador_canal_externo"})
})
@Getter
@Setter
@NoArgsConstructor
public class ReservaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigo;                               // RES-YYYY-NNNNN

    @Column(nullable = false)
    private String apartamentoId;

    @Embedded
    private EstanciaEmbedable estancia;

    @Embedded
    private TitularEmbedable titular;

    @ElementCollection
    @CollectionTable(name = "reserva_ocupante", joinColumns = @JoinColumn(name = "reserva_id"))
    @OrderColumn(name = "posicion")                      // conserva el orden en que se registraron
    private List<OcupanteEmbedable> ocupantes = new ArrayList<>();

    @Enumerated(EnumType.STRING)                         // guarda "PORTAL", no 0
    @Column(name = "canal_origen", nullable = false)
    private CanalOrigen canalOrigen;

    @Column(name = "identificador_canal_externo")
    private String identificadorCanalExterno;            // RN-19

    private LocalTime horaEstimadaLlegada;               // RN-09, RP-03

    // Cotización congelada (RN-22)
    @ElementCollection
    @CollectionTable(name = "reserva_detalle_noche", joinColumns = @JoinColumn(name = "reserva_id"))
    @OrderColumn(name = "posicion")
    private List<DetalleNocheEmbedable> detallesCotizacion = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal valorTotal;

    // Política de cancelación congelada (RN-13, RN-22)
    private int politicaVersion;
    private LocalDate politicaFechaVigencia;
    private String politicaConsecuenciaNoShow;

    @ElementCollection
    @CollectionTable(name = "reserva_tramo_cancelacion", joinColumns = @JoinColumn(name = "reserva_id"))
    @OrderColumn(name = "posicion")
    private List<TramoCancelacionEmbedable> politicaTramos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaConfirmacion;
    private LocalDate fechaRegistro;
    private LocalDate fechaSalidaEfectiva;
    private LocalDate fechaCancelacion;
    private String motivoCancelacion;
}