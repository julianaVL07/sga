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

import java.time.LocalDateTime;

/**
 * Fila de la tabla canal_venta (10.3.1). Guarda el hash BCrypt de la credencial, nunca la credencial en claro.
 * Las filas no se borran: activo=false es la eliminación lógica.
 */
@Entity
@Table(name = "canal_venta")
@Getter
@Setter
@NoArgsConstructor
public class CanalVentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalOrigen tipo;

    @Column(nullable = false, length = 100)
    private String hashCredencial;

    private boolean activo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;
}
