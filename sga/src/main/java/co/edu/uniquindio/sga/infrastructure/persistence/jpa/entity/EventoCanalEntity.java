package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoEventoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoEventoCanal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Fila de la tabla evento_canal: la bitácora de mensajes con los canales (7.8). Solo se insertan filas.
 * El índice por canal e identificador externo acelera la búsqueda de duplicados (RN-19); no es único porque
 * la misma reserva externa tiene varios eventos (creación, cancelación, rechazos).
 */
@Entity
@Table(name = "evento_canal", indexes = @Index(name = "ix_evento_canal_externo",
        columnList = "canal_origen, identificador_externo"))
@Getter
@Setter
@NoArgsConstructor
public class EventoCanalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEventoCanal tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_origen", nullable = false)
    private CanalOrigen canalOrigen;

    @Column(name = "identificador_externo")
    private String identificadorExterno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoEventoCanal resultado;

    @Column(nullable = false)
    private LocalDateTime fecha;
}