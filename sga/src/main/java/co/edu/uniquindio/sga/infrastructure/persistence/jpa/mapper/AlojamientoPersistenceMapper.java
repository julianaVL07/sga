package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.valueobject.CargoAseoMascota;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstanciaMinima;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.AlojamientoEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.RangoFechasEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ServicioAdicionalEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TemporadaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Traduce a mano el agregado Alojamiento con sus temporadas y servicios adicionales (Guía 09, 6.6).
 * Al leer usa las fábricas reconstruir: recrean el estado guardado sin repetir las validaciones de creación.
 */
@Component
public class AlojamientoPersistenceMapper {

    // ALOJAMIENTO

    public AlojamientoEntity aEntidad(Alojamiento a) {
        AlojamientoEntity e = new AlojamientoEntity();
        e.setAlojamientoId(a.getId());
        e.setNombre(a.getNombre());
        e.setDescripcion(a.getDescripcion());
        e.setCiudad(a.getCiudad());
        e.setDireccion(a.getDireccion());
        e.setLatitud(a.getUbicacion().latitud());
        e.setLongitud(a.getUbicacion().longitud());
        e.setHoraEntrada(a.getHoraEntrada());
        e.setHoraSalida(a.getHoraSalida());
        e.setNormaConvivencia(a.getNormaConvivencia());

        e.setUmbralEdadFacturable(a.getUmbralEdadFacturable() == null ? null : a.getUmbralEdadFacturable().anios());
        e.setTiempoPreparacionHoras(a.getTiempoPreparacion() == null ? null : a.getTiempoPreparacion().horas());
        e.setPlazoConfirmacionHoras(a.getPlazoConfirmacion() == null ? null : a.getPlazoConfirmacion().horas());
        e.setHoraLimiteNoShow(a.getHoraLimiteNoShow());
        e.setHoraInicioLlegadaNocturna(a.getHoraInicioLlegadaNocturna());
        if (a.getEstanciaMinima() != null) {
            e.setEstanciaMinimaNoches(a.getEstanciaMinima().nochesMinimas());
            e.setEstanciaMinimaAplicaTemporadaAltaMedia(a.getEstanciaMinima().aplicaTemporadaAltaMedia());
        }
        e.setExigeAnticipo(a.isExigeAnticipo());
        e.setPorcentajeAnticipo(a.getPorcentajeAnticipo());

        CargoAseoMascota mascotas = a.getPoliticaMascotas();
        e.setMascotasAplica(mascotas.aplica());
        e.setMascotasValor(mascotas.valor() == null ? null : mascotas.valor().monto());
        e.setMascotasMaximo(mascotas.maximoMascotas());
        e.setMascotasPesoMaximoKg(mascotas.pesoMaximoKg());

        e.setApartamentos(new ArrayList<>(a.getApartamentos().stream().map(IdentificacionApartamento::codigo).toList()));
        return e;
    }

    public Alojamiento aDominio(AlojamientoEntity e, List<TemporadaEntity> temporadas,
                                List<ServicioAdicionalEntity> servicios) {
        return Alojamiento.reconstruir(
                e.getAlojamientoId(), e.getNombre(), e.getDescripcion(), e.getCiudad(), e.getDireccion(),
                new Coordenada(e.getLatitud(), e.getLongitud()), e.getHoraEntrada(), e.getHoraSalida(),
                e.getNormaConvivencia(),
                e.getUmbralEdadFacturable() == null ? null : new UmbralEdadFacturable(e.getUmbralEdadFacturable()),
                e.getTiempoPreparacionHoras() == null ? null : new TiempoPreparacion(e.getTiempoPreparacionHoras()),
                e.getPlazoConfirmacionHoras() == null ? null : new PlazoConfirmacion(e.getPlazoConfirmacionHoras()),
                e.getHoraLimiteNoShow(), e.getHoraInicioLlegadaNocturna(),
                e.getEstanciaMinimaNoches() == null ? null
                        : new EstanciaMinima(e.getEstanciaMinimaNoches(), e.isEstanciaMinimaAplicaTemporadaAltaMedia()),
                e.isExigeAnticipo(), e.getPorcentajeAnticipo(),
                new CargoAseoMascota(e.isMascotasAplica(),
                        e.getMascotasValor() == null ? Dinero.cero() : new Dinero(e.getMascotasValor()),
                        e.getMascotasMaximo(), e.getMascotasPesoMaximoKg()),
                e.getApartamentos().stream().map(IdentificacionApartamento::new).toList(),
                temporadas.stream().map(this::aDominio).toList(),
                servicios.stream().map(this::aDominio).toList());
    }

    //  TEMPORADA

    public TemporadaEntity aEntidad(Temporada t) {
        TemporadaEntity e = new TemporadaEntity();
        e.setTemporadaId(t.getId());
        e.setNombre(t.getNombre());
        e.setTipo(t.getTipo());
        e.setRangos(new ArrayList<>(t.getRangos().stream()
                .map(r -> new RangoFechasEmbedable(r.fechaInicio(), r.fechaFin()))
                .toList()));
        return e;
    }

    public Temporada aDominio(TemporadaEntity e) {
        return Temporada.reconstruir(e.getTemporadaId(), e.getNombre(), e.getTipo(),
                e.getRangos().stream().map(r -> new RangoFechas(r.getFechaInicio(), r.getFechaFin())).toList());
    }

    //  SERVICIO ADICIONAL

    public ServicioAdicionalEntity aEntidad(ServicioAdicional s) {
        ServicioAdicionalEntity e = new ServicioAdicionalEntity();
        e.setServicioId(s.getId());
        e.setNombre(s.getNombre());
        e.setDescripcion(s.getDescripcion());
        e.setGeneraCargo(s.isGeneraCargo());
        e.setValor(s.getValor() == null ? null : s.getValor().monto());
        e.setActivo(s.isActivo());
        return e;
    }

    public ServicioAdicional aDominio(ServicioAdicionalEntity e) {
        return ServicioAdicional.reconstruir(e.getServicioId(), e.getNombre(), e.getDescripcion(), e.isGeneraCargo(),
                e.getValor() == null ? null : new Dinero(e.getValor()), e.isActivo());
    }
}