package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.request.ImagenRequest;
import co.edu.uniquindio.sga.application.dto.request.TarifaRequest;
import co.edu.uniquindio.sga.application.dto.response.ActualizacionApartamentoResponse;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoDetalleResponse;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoDisponibleResponse;
import co.edu.uniquindio.sga.application.dto.response.ApartamentoResponse;
import co.edu.uniquindio.sga.application.dto.response.BloqueoResponse;
import co.edu.uniquindio.sga.application.dto.response.CotizacionResponse;
import co.edu.uniquindio.sga.application.dto.response.DetalleNocheResponse;
import co.edu.uniquindio.sga.application.dto.response.NovedadResponse;
import co.edu.uniquindio.sga.application.dto.response.RegistroTarifaResponse;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.valueobject.ApartamentoDisponible;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Cotizacion;
import co.edu.uniquindio.sga.domain.valueobject.DetalleNoche;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Imagen;
import co.edu.uniquindio.sga.domain.valueobject.RegistroTarifa;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoActualizacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Mapper REST de apartamentos y operación (Guía 08): Apartamento, Cotizacion, Bloqueo y Novedad a sus Response.
 * MapStruct genera ApartamentoMapperImpl al compilar y lo registra como bean (componentModel = "spring").
 * Los objetos de valor se aplanan: "APT-101" como texto, Capacidad como entero y Dinero como pesos enteros.
 */
@Mapper(componentModel = "spring")
public interface ApartamentoMapper {

    // ==================== APARTAMENTO (CU-01, CU-16) ====================

    @Mapping(target = "identificacion", source = "identificacion.codigo")
    @Mapping(target = "capacidadMaxima", source = "capacidad.maximo")
    @Mapping(target = "dotacion", source = "dotacion.elementos")
    @Mapping(target = "imagenes", source = "imagenes")
    ApartamentoResponse aResponse(Apartamento apartamento);

    List<ApartamentoResponse> aResponses(List<Apartamento> apartamentos);

    // ==================== RESULTADO DE LA BÚSQUEDA (CU-01, 7.5) ====================

    @Mapping(target = "identificacion", source = "apartamento.identificacion.codigo")
    @Mapping(target = "nombre", source = "apartamento.nombre")
    @Mapping(target = "imagenPrincipal", expression = "java(aUrl(disponible.apartamento().imagenPrincipal()))")
    @Mapping(target = "capacidadMaxima", source = "apartamento.capacidad.maximo")
    @Mapping(target = "valorEstimado", source = "valorEstimado")
    @Mapping(target = "latitud", source = "ubicacion.latitud")
    @Mapping(target = "longitud", source = "ubicacion.longitud")
    ApartamentoDisponibleResponse aDisponibleResponse(ApartamentoDisponible disponible);

    // ==================== DETALLE (CU-20, CU-21) ====================

    /** Solo los bloqueos vigentes; las imágenes conservan cuál es la principal. */
    default ApartamentoDetalleResponse aDetalleResponse(Apartamento apartamento) {
        if (apartamento == null) {
            return null;
        }
        return new ApartamentoDetalleResponse(
                apartamento.getIdentificacion().codigo(),
                apartamento.getNombre(),
                apartamento.getDescripcion(),
                apartamento.getDormitorios(),
                apartamento.getCapacidad().maximo(),
                apartamento.getDotacion().elementos(),
                apartamento.getImagenes().stream()
                        .map(i -> new ApartamentoDetalleResponse.ImagenResponse(i.url(), i.esPrincipal()))
                        .toList(),
                aUrl(apartamento.imagenPrincipal()),
                apartamento.getEstadoOperativo(),
                apartamento.isActivo(),
                apartamento.isEliminado(),
                apartamento.getTarifas().stream().map(this::aTarifaResponse).toList(),
                apartamento.getBloqueos().stream().filter(Bloqueo::isVigente).map(this::aBloqueoResponse).toList());
    }

    default ApartamentoDetalleResponse.TarifaResponse aTarifaResponse(Tarifa tarifa) {
        return new ApartamentoDetalleResponse.TarifaResponse(tarifa.idTemporada(), aPesos(tarifa.valorPorOcupante()));
    }

    /** Cada imagen viaja como su URL. */
    default String aUrl(Imagen imagen) {
        return imagen == null ? null : imagen.url();
    }

    // ==================== ADMINISTRACIÓN Y TARIFAS (CU-22 a CU-25) ====================

    /** CU-22: cada imagen del request a su objeto de valor; la URL y la principal única las valida el dominio. */
    default List<Imagen> aImagenes(List<ImagenRequest> imagenes) {
        return imagenes == null ? List.of() : imagenes.stream().map(i -> new Imagen(i.url(), i.principal())).toList();
    }

    /** CU-22 y CU-24: pesos enteros a Dinero; el valor mayor que cero lo valida Tarifa (7.4). */
    default List<Tarifa> aTarifas(List<TarifaRequest> tarifas) {
        return tarifas == null ? List.of() : tarifas.stream()
                .map(t -> new Tarifa(t.idTemporada(), Dinero.de(t.valorPorOcupante())))
                .toList();
    }

    /** CU-23 (7.3): el apartamento y, si hay reservas por encima de la nueva capacidad, la advertencia. */
    default ActualizacionApartamentoResponse aActualizacionResponse(ResultadoActualizacionApartamento resultado) {
        List<String> codigos = resultado.reservasSobreCapacidad().stream().map(CodigoReserva::valor).toList();
        String advertencia = resultado.tieneAdvertencia()
                ? "7.3: " + codigos.size() + " reserva(s) activa(s) o futura(s) superan la nueva capacidad de "
                        + resultado.apartamento().getCapacidad().maximo()
                        + " ocupantes; las reservas no se modifican"
                : null;
        return new ActualizacionApartamentoResponse(aDetalleResponse(resultado.apartamento()), codigos, advertencia);
    }

    /** CU-24 (7.4): una entrada del histórico; vigenteHasta null indica la tarifa que rige hoy. */
    default RegistroTarifaResponse aRegistroTarifaResponse(RegistroTarifa registro) {
        return new RegistroTarifaResponse(registro.idTemporada(), aPesos(registro.valorPorOcupante()),
                registro.vigenteDesde(), registro.vigenteHasta(), registro.estaVigente());
    }

    // ==================== COTIZACIÓN (CU-02) ====================

    /** RN-05: el detalle noche a noche; el total es la suma de los subtotales. */
    default CotizacionResponse aCotizacionResponse(String identificacionApartamento, LocalDate fechaEntrada,
                                                   LocalDate fechaSalida, Cotizacion cotizacion) {
        return new CotizacionResponse(identificacionApartamento, fechaEntrada, fechaSalida,
                cotizacion.totalNoches(), aDetalles(cotizacion.detalles()), aPesos(cotizacion.valorTotal()));
    }

    DetalleNocheResponse aDetalleResponse(DetalleNoche detalle);

    List<DetalleNocheResponse> aDetalles(List<DetalleNoche> detalles);

    // ==================== BLOQUEO (CU-13) ====================

    @Mapping(target = "identificacionApartamento", source = "apartamento.codigo")
    @Mapping(target = "fechaInicio", source = "rango.fechaInicio")
    @Mapping(target = "fechaFin", source = "rango.fechaFin")
    BloqueoResponse aBloqueoResponse(Bloqueo bloqueo);

    // ==================== NOVEDAD (CU-17) ====================

    @Mapping(target = "identificacionApartamento", source = "apartamento.codigo")
    NovedadResponse aNovedadResponse(Novedad novedad);

    // ==================== OBJETOS DE VALOR ====================

    /** Dinero viaja como pesos enteros, nunca como double. */
    default long aPesos(Dinero dinero) {
        return dinero == null ? 0 : dinero.monto().setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}