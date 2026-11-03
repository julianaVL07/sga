package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Titular de la reserva (3.2) como columnas de la tabla reserva. Su documento es obligatorio. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TitularEmbedable {

    @Column(name = "titular_nombre", nullable = false)
    private String nombre;

    @Column(name = "titular_tipo_documento", nullable = false)
    private String tipoDocumento;

    @Column(name = "titular_numero_documento", nullable = false)
    private String numeroDocumento;

    @Column(name = "titular_email", nullable = false)
    private String email;

    @Column(name = "titular_telefono", nullable = false)
    private String telefono;
}