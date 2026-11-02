package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.service.CreacionTemporadaService.TarifaApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.RegistroTarifa;
import co.edu.uniquindio.sga.domain.valueobject.TipoTemporada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de trazabilidad: CU-29 y 7.4 (todo apartamento activo tiene tarifa en todas las temporadas, temporadas sin
 * solaparse) y F-05 (una sola temporada base), implementadas en CreacionTemporadaService y Alojamiento.agregarTemporada.
 * Escenario: Coral con temporada base y alta (5 al 14 de octubre de 2026) y APT-101 activo.
 */
class CreacionTemporadaServiceTest {

    private static final LocalDate HOY = EscenarioCoral.AHORA.toLocalDate();
    private static final IdentificacionApartamento APT_102 = new IdentificacionApartamento("APT-102");
    /** Fiestas de Independencia: del 6 al 16 de noviembre de 2026 (fecha final excluida). */
    private static final RangoFechas NOVIEMBRE = new RangoFechas(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 17));

    private final EscenarioCoral escenario = new EscenarioCoral();
    private final CreacionTemporadaService creacion = new CreacionTemporadaService(escenario.alojamientos,
            escenario.apartamentos);

    @Test
    @DisplayName("7.4: con la tarifa de todos los apartamentos activos se crea la temporada y cada tarifa queda en "
            + "el histórico")
    void crearConTarifasCompletas() {
        Temporada media = creacion.crear("T-MEDIA", "Media", TipoTemporada.MEDIA, List.of(NOVIEMBRE),
                List.of(tarifa(EscenarioCoral.APT_101, 430_000)), HOY);

        Alojamiento alojamiento = escenario.alojamientos.obtener().orElseThrow();
        assertEquals(3, alojamiento.getTemporadas().size());
        assertEquals(media, alojamiento.obtenerTemporadaPara(LocalDate.of(2026, 11, 10)));
        Apartamento apto = apartamento(EscenarioCoral.APT_101);
        assertEquals(Dinero.de(430_000), apto.tarifaPara(media).valorPorOcupante());
        assertTrue(apto.tieneTarifasCompletas(alojamiento.getTemporadas()));
        RegistroTarifa registro = apto.getHistoricoTarifas().stream()
                .filter(r -> r.esDeTemporada("T-MEDIA")).findFirst().orElseThrow();
        assertEquals(HOY, registro.vigenteDesde());
        assertTrue(registro.estaVigente());
    }

    @Test
    @DisplayName("7.4: si falta la tarifa de un apartamento activo no se crea la temporada ni se asigna ninguna tarifa")
    void faltaTarifaDeApartamentoActivo() {
        Apartamento inactivo = registrarInactivo();

        ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> creacion.crear("T-MEDIA",
                "Media", TipoTemporada.MEDIA, List.of(NOVIEMBRE), List.of(tarifa(inactivo.getIdentificacion(), 400_000)),
                HOY));

        assertTrue(error.getMessage().startsWith("7.4"));
        assertTrue(error.getMessage().contains("APT-101"));
        assertEquals(2, escenario.alojamientos.obtener().orElseThrow().getTemporadas().size());
        assertEquals(2, apartamento(EscenarioCoral.APT_101).getTarifas().size());
        assertTrue(apartamento(APT_102).getTarifas().stream().noneMatch(t -> t.idTemporada().equals("T-MEDIA")));
    }

    @Test
    @DisplayName("7.4: un apartamento inactivo no exige tarifa, pero si se envía también la recibe")
    void apartamentoInactivoNoExigeTarifa() {
        registrarInactivo();

        creacion.crear("T-MEDIA", "Media", TipoTemporada.MEDIA, List.of(NOVIEMBRE),
                List.of(tarifa(EscenarioCoral.APT_101, 430_000)), HOY);
        creacion.crear("T-FERIA", "Feria", TipoTemporada.ALTA,
                List.of(new RangoFechas(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5))),
                List.of(tarifa(EscenarioCoral.APT_101, 600_000), tarifa(APT_102, 500_000)), HOY);

        assertTrue(apartamento(APT_102).getTarifas().stream().anyMatch(t -> t.idTemporada().equals("T-FERIA")));
        assertTrue(apartamento(APT_102).getTarifas().stream().noneMatch(t -> t.idTemporada().equals("T-MEDIA")));
    }

    @Test
    @DisplayName("7.4: una temporada que se solapa con otra se rechaza y no asigna tarifas")
    void temporadaSolapada() {
        RangoFechas dentroDeAlta = new RangoFechas(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20));

        ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> creacion.crear("T-MEDIA",
                "Media", TipoTemporada.MEDIA, List.of(dentroDeAlta), List.of(tarifa(EscenarioCoral.APT_101, 430_000)),
                HOY));

        assertTrue(error.getMessage().startsWith("7.4"));
        assertTrue(error.getMessage().contains("se solapa"));
        assertEquals(2, apartamento(EscenarioCoral.APT_101).getTarifas().size());
    }

    @Test
    @DisplayName("F-05: no se crea una segunda temporada base")
    void segundaBase() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> creacion.crear("T-BASE-2",
                "Otra base", TipoTemporada.BASE, List.of(), List.of(tarifa(EscenarioCoral.APT_101, 300_000)), HOY));

        assertTrue(error.getMessage().startsWith("F-05"));
        assertEquals(2, escenario.alojamientos.obtener().orElseThrow().getTemporadas().size());
    }

    @Test
    @DisplayName("F-05: la temporada base no lleva rangos")
    void baseConRangos() {
        ReglaDominioException error = assertThrows(ReglaDominioException.class, () -> creacion.crear("T-BASE-2",
                "Otra base", TipoTemporada.BASE, List.of(NOVIEMBRE), List.of(), HOY));

        assertTrue(error.getMessage().startsWith("F-05"));
    }

    @Test
    @DisplayName("7.4: las tarifas deben ser de apartamentos existentes, una por apartamento y mayores que cero")
    void tarifasInvalidas() {
        ReglaDominioException inexistente = assertThrows(ReglaDominioException.class, () -> crearMedia(List.of(
                tarifa(EscenarioCoral.APT_101, 430_000), tarifa(new IdentificacionApartamento("APT-999"), 430_000))));
        assertTrue(inexistente.getMessage().contains("APT-999"));

        ReglaDominioException repetida = assertThrows(ReglaDominioException.class, () -> crearMedia(List.of(
                tarifa(EscenarioCoral.APT_101, 430_000), tarifa(EscenarioCoral.APT_101, 450_000))));
        assertTrue(repetida.getMessage().contains("más de una vez"));

        ReglaDominioException enCero = assertThrows(ReglaDominioException.class,
                () -> crearMedia(List.of(tarifa(EscenarioCoral.APT_101, 0))));
        assertTrue(enCero.getMessage().startsWith("7.4"));

        assertFalse(escenario.alojamientos.obtener().orElseThrow().buscarTemporada("T-MEDIA").isPresent());
    }

    private void crearMedia(List<TarifaApartamento> tarifas) {
        creacion.crear("T-MEDIA", "Media", TipoTemporada.MEDIA, List.of(NOVIEMBRE), tarifas, HOY);
    }

    /** APT-102 recién creado: nace inactivo (7.4). */
    private Apartamento registrarInactivo() {
        Apartamento nuevo = Apartamento.crear(APT_102, "Apartamento 102", null, 1, new Capacidad(2),
                new Dotacion(List.of("Aire acondicionado")), List.of(new Imagen("https://coral.co/apt-102.jpg", true)));
        escenario.apartamentos.guardar(nuevo);
        return nuevo;
    }

    private Apartamento apartamento(IdentificacionApartamento identificacion) {
        return escenario.apartamentos.buscarPorIdentificacion(identificacion).orElseThrow();
    }

    private static TarifaApartamento tarifa(IdentificacionApartamento apartamento, long pesos) {
        return new TarifaApartamento(apartamento, Dinero.de(pesos));
    }
}
