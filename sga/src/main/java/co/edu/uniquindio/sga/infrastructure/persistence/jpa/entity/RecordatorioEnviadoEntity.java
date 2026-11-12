package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.MotivoRecordatorio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fila de la tabla recordatorio_enviado (7.8): una por reserva y motivo, con el último envío. */
@Entity
@Table(name = "recordatorio_enviado",
        uniqueConstraints = @UniqueConstraint(columnNames = {"codigo_reserva", "motivo"}))
@Getter
@Setter
@NoArgsConstructor
public class RecordatorioEnviadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_reserva", nullable = false, length = 20)
    private String codigoReserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MotivoRecordatorio motivo;

    @Column(nullable = false)
    private LocalDate fechaEvento;

    @Column(nullable = false)
    private LocalDateTime enviadoEn;
}
