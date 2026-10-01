package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Matriz de trazabilidad: RN-02, implementada en Apartamento.admite() y Apartamento.validarCapacidadPara(). */
class ApartamentoTest {

    @Test
    @DisplayName("RN-02: un apartamento de capacidad 3 rechaza un grupo de 4 y admite uno de 3")
    void rechazarGrupoQueSuperaCapacidadMaxima() {
        Apartamento apartamento = Apartamento.crear(new IdentificacionApartamento("APT-101"), "Apartamento 101",
                "Vista al mar", 1, new Capacidad(3), new Dotacion(List.of("Aire acondicionado")),
                List.of(new Imagen("https://coral.co/apt-101.jpg", true)));

        assertFalse(apartamento.admite(4));
        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> apartamento.validarCapacidadPara(4));
        assertTrue(error.getMessage().startsWith("RN-02"));

        assertTrue(apartamento.admite(3));
        assertDoesNotThrow(() -> apartamento.validarCapacidadPara(3));
    }
}
