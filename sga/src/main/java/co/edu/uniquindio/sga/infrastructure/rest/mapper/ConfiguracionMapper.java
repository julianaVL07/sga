package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.request.ActualizarAlojamientoRequest;
import co.edu.uniquindio.sga.application.dto.request.CrearTemporadaRequest;
import co.edu.uniquindio.sga.application.dto.request.RangoFechasRequest;
import co.edu.uniquindio.sga.application.dto.request.TramoCancelacionRequest;
import co.edu.uniquindio.sga.application.dto.response.AlojamientoResponse;
import co.edu.uniquindio.sga.application.dto.response.PoliticaCancelacionResponse;
import co.edu.uniquindio.sga.application.dto.response.TemporadaResponse;
import co.edu.uniquindio.sga.application.usecase.ConfiguracionAlojamientoUseCase;
import co.edu.uniquindio.sga.application.usecase.TemporadasUseCase;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.service.CreacionTemporadaService.TarifaApartamento;
import co.edu.uniquindio.sga.domain.valueobject.CargoAseoMascota;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.DepositoGarantia;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.valueobject.RangoFechas;
import co.edu.uniquindio.sga.domain.valueobject.TramoCancelacion;
import org.mapstruct.Mapper;

import java.math.RoundingMode;
import java.util.List;

/**
 * Mapper REST de la configuración del alojamiento (CU-28 a CU-30): Alojamiento, Temporada y PoliticaCancelacion a
 * sus Response, y los Request a objetos del dominio. MapStruct genera ConfiguracionMapperImpl al compilar y lo
 * registra como bean (componentModel = "spring"). Las reglas no viven aquí: si un dato viola una regla, la rechaza
 * el constructor del objeto de valor (Coordenada, RangoFechas, TramoCancelacion) o el agregado.
 */
@Mapper(componentModel = "spring")
public interface ConfiguracionMapper {

    // ==================== ALOJAMIENTO (CU-28) ====================

    default ConfiguracionAlojamientoUseCase.DatosAlojamiento aDatos(ActualizarAlojamientoRequest r) {
        return new ConfiguracionAlojamientoUseCase.DatosAlojamiento(r.nombre(), r.descripcion(), r.ciudad(),
                r.direccion(), new Coordenada(r.latitud(), r.longitud()), r.normaConvivencia(), r.horaEntrada(),
                r.horaSalida());
    }

    /**
     * El depósito de garantía (RP-02) no es parte del agregado Alojamiento: llega del bean DepositoGarantia, el mismo
     * que usa el folio, para no duplicar el valor configurado.
     */
    default AlojamientoResponse aAlojamientoResponse(Alojamiento a, DepositoGarantia deposito) {
        if (a == null) {
            return null;
        }
        CargoAseoMascota mascotas = a.getPoliticaMascotas();
        AlojamientoResponse.ParametrosResponse parametros = new AlojamientoResponse.ParametrosResponse(
                a.getUmbralEdadFacturable() == null ? null : a.getUmbralEdadFacturable().anios(),
                a.getTiempoPreparacion() == null ? null : a.getTiempoPreparacion().horas(),
                a.getPlazoConfirmacion() == null ? null : a.getPlazoConfirmacion().horas(),
                a.getHoraLimiteNoShow(),
                a.getHoraInicioLlegadaNocturna(),
                a.getEstanciaMinima() == null ? null : a.getEstanciaMinima().nochesMinimas(),
                a.getEstanciaMinima() != null && a.getEstanciaMinima().aplicaTemporadaAltaMedia(),
                a.isExigeAnticipo(),
                a.getPorcentajeAnticipo(),
                mascotas.aplica(),
                aPesos(mascotas.valor()),
                mascotas.maximoMascotas(),
                mascotas.pesoMaximoKg(),
                deposito == null ? 0 : aPesos(deposito.monto()));
        return new AlojamientoResponse(a.getId(), a.getNombre(), a.getDescripcion(), a.getCiudad(),
                a.getDireccion(), a.getUbicacion().latitud(), a.getUbicacion().longitud(), a.getHoraEntrada(),
                a.getHoraSalida(), a.getNormaConvivencia(),
                a.getServiciosAdicionales().stream().map(this::aServicioResponse).toList(),
                parametros);
    }

    default AlojamientoResponse.ServicioAdicionalResponse aServicioResponse(ServicioAdicional s) {
        return new AlojamientoResponse.ServicioAdicionalResponse(s.getId(), s.getNombre(), s.getDescripcion(),
                s.isGeneraCargo(), aPesos(s.getValor()), s.isActivo());
    }

    // ==================== TEMPORADAS (CU-29) ====================

    /** Las listas que no llegan van vacías: la regla de cuántos rangos y tarifas hacen falta es del dominio (7.4). */
    default TemporadasUseCase.SolicitudTemporada aSolicitud(CrearTemporadaRequest r) {
        List<RangoFechas> rangos = r.rangos() == null ? List.of() : r.rangos().stream().map(this::aRango).toList();
        List<TarifaApartamento> tarifas = r.tarifas() == null ? List.of() : r.tarifas().stream()
                .map(t -> new TarifaApartamento(new IdentificacionApartamento(t.identificacionApartamento()),
                        Dinero.de(t.valorPorOcupante())))
                .toList();
        return new TemporadasUseCase.SolicitudTemporada(r.id(), r.nombre(), r.tipo(), rangos, tarifas);
    }

    default RangoFechas aRango(RangoFechasRequest r) {
        return new RangoFechas(r.fechaInicio(), r.fechaFin());
    }

    default TemporadaResponse aTemporadaResponse(Temporada t) {
        if (t == null) {
            return null;
        }
        return new TemporadaResponse(t.getId(), t.getNombre(), t.getTipo(), t.getRangos().stream()
                .map(r -> new TemporadaResponse.RangoResponse(r.fechaInicio(), r.fechaFin()))
                .toList());
    }

    // ==================== POLÍTICA DE CANCELACIÓN (CU-30) ====================

    default List<TramoCancelacion> aTramos(List<TramoCancelacionRequest> tramos) {
        return tramos == null ? List.of() : tramos.stream()
                .map(t -> new TramoCancelacion(t.antelacionMinimaDias(), t.porcentajeRetencion()))
                .toList();
    }

    default PoliticaCancelacionResponse aPoliticaResponse(PoliticaCancelacion p) {
        if (p == null) {
            return null;
        }
        return new PoliticaCancelacionResponse(p.version(), p.fechaVigencia(), p.consecuenciaNoShow(),
                p.tramos().stream()
                        .map(t -> new PoliticaCancelacionResponse.TramoResponse(t.antelacionMinimaDias(),
                                t.porcentajeRetencion()))
                        .toList());
    }

    // ==================== OBJETOS DE VALOR ====================

    /** Dinero viaja como pesos enteros, nunca como double. */
    default long aPesos(Dinero dinero) {
        return dinero == null ? 0 : dinero.monto().setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
