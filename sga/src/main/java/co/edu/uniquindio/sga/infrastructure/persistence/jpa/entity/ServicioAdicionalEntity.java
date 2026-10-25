package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Fila de la tabla servicio_adicional (L-17). Pertenece al único alojamiento (F-13). */
@Entity
@Table(name = "servicio_adicional")
@Getter
@Setter
@NoArgsConstructor
public class ServicioAdicionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String servicioId;                           // SRV-RECEPCION-NOCTURNA

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    private boolean generaCargo;

    private BigDecimal valor;                            // null si no genera cargo

    private boolean activo;
}