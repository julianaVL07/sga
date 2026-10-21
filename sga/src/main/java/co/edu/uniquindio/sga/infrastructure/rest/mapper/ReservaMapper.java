package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import co.edu.uniquindio.sga.application.dto.request.CrearReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.OcupanteRequest;
import co.edu.uniquindio.sga.application.dto.request.TitularRequest;
import co.edu.uniquindio.sga.application.dto.response.OcupanteResponse;
import co.edu.uniquindio.sga.application.dto.response.ReservaDetalleResponse;
import co.edu.uniquindio.sga.application.dto.response.TitularResponse;
import co.edu.uniquindio.sga.application.usecase.CrearReservaUseCase;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Ocupante;
import co.edu.uniquindio.sga.domain.valueobject.Titular;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.RoundingMode;
import java.util.List;

/**
 * Mapper REST del agregado Reserva (Guía 08): traduce DTOs a objetos del dominio y la entidad Reserva a su Response.
 * MapStruct genera ReservaMapperImpl al compilar y lo registra como bean (componentModel = "spring").
 * Las reglas no viven aquí: si un dato viola RN-xx, lo rechaza el constructor del objeto de valor o el dominio.
 */
@Mapper(componentModel = "spring")
public interface ReservaMapper {

    // ==================== REQUEST → DOMINIO ====================

    /** El titular trae todos sus datos obligatorios (3.2). */
    default Titular aTitular(TitularRequest r) {
        if (r == null) return null;
        return new Titular(r.nombre(), new DocumentoIdentidad(r.tipoDocumento(), r.numeroDocumento()),
                r.email(), r.telefono());
    }

    /** El documento del ocupante es opcional (3.2, 7.2): si no viene número, queda en null. */
    default Ocupante aOcupante(OcupanteRequest r) {
        if (r == null) return null;
        DocumentoIdentidad documento = (r.numeroDocumento() == null || r.numeroDocumento().isBlank())
                ? null
                : new DocumentoIdentidad(r.tipoDocumento(), r.numeroDocumento());
        return new Ocupante(r.nombre(), documento, r.fechaNacimiento());
    }

    /** MapStruct recorre la lista usando aOcupante. */
    List<Ocupante> aOcupantes(List<OcupanteRequest> requests);

    /** La identificación del apartamento llega como texto ("APT-101"); null se conserva (CU-11). */
    default IdentificacionApartamento aIdentificacion(String identificacion) {
        return (identificacion == null || identificacion.isBlank()) ? null : new IdentificacionApartamento(identificacion);
    }

    /**
     * CU-03. La cotización va en null: CreacionReservaService la calcula y la congela (RN-05, RN-06, RN-22).
     */
    default CrearReservaUseCase.SolicitudReserva aSolicitud(CrearReservaRequest r) {
        return new CrearReservaUseCase.SolicitudReserva(
                aIdentificacion(r.identificacionApartamento()),
                r.fechaEntrada(), r.fechaSalida(),
                aTitular(r.titular()), aOcupantes(r.ocupantes()),
                r.horaEstimadaLlegada(), null, r.canalOrigen());
    }

    // ==================== DOMINIO → RESPONSE ====================

    @Mapping(target = "codigo", source = "codigo.valor")
    @Mapping(target = "identificacionApartamento", source = "apartamento.codigo")
    @Mapping(target = "fechaEntrada", source = "estancia.fechaEntrada")
    @Mapping(target = "fechaSalida", source = "estancia.fechaSalida")
    @Mapping(target = "noches", expression = "java(reserva.getEstancia().noches())")
    @Mapping(target = "totalOcupantes", expression = "java(reserva.totalOcupantes())")
    @Mapping(target = "valorTotal", expression = "java(aPesos(reserva.valorTotal()))")
    @Mapping(target = "versionPoliticaCancelacion", source = "politicaCancelacion.version")
    ReservaDetalleResponse aDetalle(Reserva reserva);

    @Mapping(target = "tipoDocumento", source = "documento.tipo")
    @Mapping(target = "numeroDocumento", source = "documento.numero")
    TitularResponse aTitularResponse(Titular titular);

    @Mapping(target = "tipoDocumento", source = "documento.tipo")      // si el documento es null, queda null
    @Mapping(target = "numeroDocumento", source = "documento.numero")
    OcupanteResponse aOcupanteResponse(Ocupante ocupante);

    // ==================== OBJETOS DE VALOR ====================

    /** Dinero viaja como pesos enteros, nunca como double. */
    default long aPesos(Dinero dinero) {
        return dinero == null ? 0 : dinero.monto().setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}