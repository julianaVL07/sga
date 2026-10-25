package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.valueobject.Capacidad;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Dotacion;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ApartamentoEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.BloqueoEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ImagenEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.RangoFechasEmbedable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TarifaEmbedable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Traduce a mano el agregado Apartamento y sus bloqueos (Guía 09, 6.6). Al leer usa las fábricas reconstruir. */
@Component
public class ApartamentoPersistenceMapper {

    //  APARTAMENTO

    public ApartamentoEntity aEntidad(Apartamento a) {
        ApartamentoEntity e = new ApartamentoEntity();
        e.setIdentificacion(a.getIdentificacion().codigo());
        e.setNombre(a.getNombre());
        e.setDescripcion(a.getDescripcion());
        e.setDormitorios(a.getDormitorios());
        e.setCapacidadMaxima(a.getCapacidad().maximo());
        e.setDotacion(new ArrayList<>(a.getDotacion().elementos()));
        e.setImagenes(new ArrayList<>(a.getImagenes().stream()
                .map(i -> new ImagenEmbedable(i.url(), i.esPrincipal()))
                .toList()));
        e.setEstadoOperativo(a.getEstadoOperativo());
        e.setActivo(a.isActivo());
        e.setEliminado(a.isEliminado());
        e.setTarifas(new ArrayList<>(a.getTarifas().stream()
                .map(t -> new TarifaEmbedable(t.idTemporada(), t.valorPorOcupante().monto()))
                .toList()));
        return e;
    }

    public Apartamento aDominio(ApartamentoEntity e, List<BloqueoEntity> bloqueos) {
        return Apartamento.reconstruir(
                new IdentificacionApartamento(e.getIdentificacion()), e.getNombre(), e.getDescripcion(),
                e.getDormitorios(), new Capacidad(e.getCapacidadMaxima()), new Dotacion(List.copyOf(e.getDotacion())),
                e.getImagenes().stream().map(i -> new Imagen(i.getUrl(), i.isEsPrincipal())).toList(),
                e.getEstadoOperativo(), e.isActivo(), e.isEliminado(),
                bloqueos.stream().map(this::aDominio).toList(),
                e.getTarifas().stream()
                        .map(t -> new Tarifa(t.getIdTemporada(), new Dinero(t.getValorPorOcupante())))
                        .toList());
    }

    //  BLOQUEO

    public BloqueoEntity aEntidad(Bloqueo b) {
        BloqueoEntity e = new BloqueoEntity();
        e.setBloqueoId(b.getId());
        e.setApartamentoId(b.getApartamento().codigo());
        e.setRango(new RangoFechasEmbedable(b.getRango().fechaInicio(), b.getRango().fechaFin()));
        e.setMotivo(b.getMotivo());
        e.setVigente(b.isVigente());
        e.setFechaLevantamiento(b.getFechaLevantamiento());
        return e;
    }

    public Bloqueo aDominio(BloqueoEntity e) {
        return Bloqueo.reconstruir(e.getBloqueoId(), new IdentificacionApartamento(e.getApartamentoId()),
                new RangoFechas(e.getRango().getFechaInicio(), e.getRango().getFechaFin()), e.getMotivo(),
                e.isVigente(), e.getFechaLevantamiento());
    }
}