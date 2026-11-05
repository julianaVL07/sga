package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.infrastructure.security.RolUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Usuario del sistema (Guía 12): concepto de acceso, vive en infraestructura (3.6) */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "contrasena", nullable = false, length = 100)
    private String contrasena; // hash BCrypt, nunca texto plano

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "documento", nullable = false, unique = true, length = 15)
    private String documento; // puente con el titular de la reserva (3.6)

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolUsuario rol;

    @Column(name = "activo", nullable = false)
    private boolean activo = true; // eliminación lógica (7.2 y 12.4)
}
