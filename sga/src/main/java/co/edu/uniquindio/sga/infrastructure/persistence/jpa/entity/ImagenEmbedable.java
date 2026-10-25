package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una imagen del apartamento (7.3), fila de la tabla hija apartamento_imagen. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImagenEmbedable {

    @Column(nullable = false, length = 500)
    private String url;

    private boolean esPrincipal;
}