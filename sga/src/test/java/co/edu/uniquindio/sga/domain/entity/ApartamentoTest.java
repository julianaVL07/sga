package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: RN-02, implementada en Apartamento.admite() y Apartamento.validarCapacidadPara();
 * 7.3 (retiro de la venta con eliminación lógica, 12.4), implementada en Apartamento.retirarDeLaVenta().
 */
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

    @Test
    @DisplayName("7.3: sin reservas activas ni futuras el apartamento se retira de la venta (eliminación lógica)")
    void retirarDeLaVentaSinReservasPendientes() {
        Apartamento apartamento = apartamentoActivo();

        apartamento.retirarDeLaVenta(false);

        assertTrue(apartamento.isEliminado());
        assertFalse(apartamento.isActivo());
        assertFalse(apartamento.estaVigenteParaVenta());
    }

    @Test
    @DisplayName("7.3: con reservas activas o futuras no se retira y queda a la venta")
    void rechazarRetiroConReservasActivasOFuturas() {
        Apartamento apartamento = apartamentoActivo();

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> apartamento.retirarDeLaVenta(true));

        assertTrue(error.getMessage().startsWith("7.3"));
        assertFalse(apartamento.isEliminado());
        assertTrue(apartamento.estaVigenteParaVenta());
    }

    @Test
    @DisplayName("7.3: un apartamento ya retirado no se retira otra vez ni puede volver a activarse")
    void rechazarRetiroDeUnApartamentoYaEliminado() {
        Apartamento apartamento = apartamentoActivo();
        apartamento.retirarDeLaVenta(false);

        ReglaDominioException error = assertThrows(ReglaDominioException.class,
                () -> apartamento.retirarDeLaVenta(false));

        assertTrue(error.getMessage().startsWith("7.3"));
        assertThrows(ReglaDominioException.class, () -> apartamento.activar(List.of(Temporada.crearBase("T-BASE", "Base"))));
    }

    private static Apartamento apartamentoActivo() {
        Apartamento apartamento = Apartamento.crear(new IdentificacionApartamento("APT-202"), "Bahía Cristal",
                "Grupos familiares", 2, new Capacidad(4), new Dotacion(List.of("Cocina integral")),
                List.of(new Imagen("https://coral.co/apt-202.jpg", true)));
        apartamento.definirTarifa(new Tarifa("T-BASE", Dinero.de(280_000)));
        apartamento.activar(List.of(Temporada.crearBase("T-BASE", "Base")));
        return apartamento;
    }
}
