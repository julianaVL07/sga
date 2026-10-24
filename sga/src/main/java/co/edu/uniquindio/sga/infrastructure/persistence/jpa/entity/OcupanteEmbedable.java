package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Un ocupante de la reserva, fila de la tabla hija reserva_ocupante (@ElementCollection).
 * El documento es opcional (3.2, 7.2): sin número, las dos columnas quedan en null.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcupanteEmbedable {

    @Column(nullable = false)
    private String nombre;

    private String tipoDocumento;

    private String numeroDocumento;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;
}
