package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.response.CargoResponse;
import co.edu.uniquindio.sga.application.dto.response.FolioResponse;
import co.edu.uniquindio.sga.application.dto.response.PagoResponse;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.RoundingMode;
import java.util.List;

/**
 * Mapper REST del folio (Guía 08): Folio, Cargo y Pago a sus Response.
 * MapStruct genera FolioMapperImpl al compilar y lo registra como bean (componentModel = "spring").
 * Los totales y el saldo los calcula el dominio (7.7); aquí solo se pasan a pesos enteros.
 */
@Mapper(componentModel = "spring")
public interface FolioMapper {

    // ==================== FOLIO (CU-06, CU-07, CU-15) ====================

    @Mapping(target = "codigoReserva", source = "reserva.valor")
    @Mapping(target = "depositoGarantia", source = "depositoGarantia.monto")
    @Mapping(target = "totalCargos", expression = "java(aPesos(folio.totalCargos()))")
    @Mapping(target = "totalPagado", expression = "java(aPesos(folio.totalPagado()))")
    @Mapping(target = "saldo", expression = "java(aPesos(folio.saldo()))")
    @Mapping(target = "autorizadoPor", source = "autorizacionCierre.autorizadoPor")   // null si no hubo autorización
    @Mapping(target = "motivoAutorizacion", source = "autorizacionCierre.motivo")
    FolioResponse aResponse(Folio folio);

    CargoResponse aCargoResponse(Cargo cargo);

    List<CargoResponse> aCargos(List<Cargo> cargos);

    PagoResponse aPagoResponse(Pago pago);

    List<PagoResponse> aPagos(List<Pago> pagos);

    // ==================== OBJETOS DE VALOR ====================

    /** Dinero viaja como pesos enteros, nunca como double. Un ajuste a la baja queda negativo (RN-14). */
    default long aPesos(Dinero dinero) {
        return dinero == null ? 0 : dinero.monto().setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}