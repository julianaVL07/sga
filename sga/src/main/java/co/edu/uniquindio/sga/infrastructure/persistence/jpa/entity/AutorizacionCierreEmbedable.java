package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Autorización del administrador para cerrar el folio con saldo distinto de cero (RN-17).
 * Sus columnas van en la tabla folio y quedan nulas mientras no haya autorización.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutorizacionCierreEmbedable {

    private String autorizadoPor;

    @Column(length = 1000)
    private String motivoAutorizacion;

    private LocalDate fechaAutorizacion;
}