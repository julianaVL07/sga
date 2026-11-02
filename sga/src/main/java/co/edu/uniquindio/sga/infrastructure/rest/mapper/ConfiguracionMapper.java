package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.request.ActualizarAlojamientoRequest;
import co.edu.uniquindio.sga.application.dto.response.AlojamientoResponse;
import co.edu.uniquindio.sga.application.usecase.ConfiguracionAlojamientoUseCase;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.valueobject.CargoAseoMascota;
import co.edu.uniquindio.sga.domain.valueobject.Coordenada;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import org.mapstruct.Mapper;

import java.math.RoundingMode;

/**
 * Mapper REST de la configuración del alojamiento (CU-28): Alojamiento a su Response y el Request a objetos del
 * dominio. MapStruct genera ConfiguracionMapperImpl al compilar y lo
 * registra como bean (componentModel = "spring"). Las reglas no viven aquí: si un dato viola una regla, la rechaza
 * el constructor del objeto de valor (Coordenada) o el agregado.
 */
@Mapper(componentModel = "spring")
public interface ConfiguracionMapper {

    // ==================== ALOJAMIENTO (CU-28) ====================

    default ConfiguracionAlojamientoUseCase.DatosAlojamiento aDatos(ActualizarAlojamientoRequest r) {
        return new ConfiguracionAlojamientoUseCase.DatosAlojamiento(r.nombre(), r.descripcion(), r.ciudad(),
                r.direccion(), new Coordenada(r.latitud(), r.longitud()), r.normaConvivencia(), r.horaEntrada(),
                r.horaSalida());
    }

    default AlojamientoResponse aAlojamientoResponse(Alojamiento a) {
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
                mascotas.pesoMaximoKg());
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

    // ==================== OBJETOS DE VALOR ====================

    /** Dinero viaja como pesos enteros, nunca como double. */
    default long aPesos(Dinero dinero) {
        return dinero == null ? 0 : dinero.monto().setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
